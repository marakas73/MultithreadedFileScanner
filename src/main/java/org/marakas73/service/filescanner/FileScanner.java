package org.marakas73.service.filescanner;

import jakarta.annotation.PreDestroy;
import org.marakas73.common.cache.CacheSizeEvaluator;
import org.marakas73.config.FileScannerProperties;
import org.marakas73.model.FileScanContext;
import org.marakas73.model.FileScanRequest;
import org.marakas73.model.FileScanResult;
import org.marakas73.service.filtermatcher.FileScanFilterMatcher;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@CacheConfig(cacheNames = "fileScanCache")
public class FileScanner {
    private static final int NORMAL_SHUTDOWN_TIME_LIMIT_SECS = 5; // 5 secs

    private final FileScanFilterMatcher patternMatcher;
    private final FileScannerProperties properties;
    private final CacheManager cacheManager;
    private final CacheSizeEvaluator cacheSizeEvaluator;

    private final ConcurrentMap<String, FileScanContext> scans = new ConcurrentHashMap<>();

    public FileScanner(
            FileScanFilterMatcher patternMatcher,
            FileScannerProperties properties,
            CacheManager cacheManager,
            CacheSizeEvaluator cacheSizeEvaluator
    ) {
        this.patternMatcher = patternMatcher;
        this.properties = properties;
        this.cacheManager = cacheManager;
        this.cacheSizeEvaluator = cacheSizeEvaluator;
    }

    public FileScanResult startScan(FileScanRequest scanRequest) {
        // Build cache key
        String cacheKey = scanRequest.directoryPath()
                + ":" + scanRequest.depthLimit()
                + ":" + scanRequest.scanFilter();

        // Check if result is already cached
        Cache cache = cacheManager.getCache("fileScanCache");
        if (cache != null) {
            // Use wrapper to avoid generics problem
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                List<String> cached = (List<String>) wrapper.get();
                if (cached != null) {
                    // Cache exits, return its value
                    // TODO: Cache log here
                    return new FileScanResult(null,true, cached);
                }
            }
        }

        // No cache found by key
        // Starting scanner task
        String id = UUID.randomUUID().toString();
        int threads = Optional.ofNullable(scanRequest.threadsCount())
                .orElse(properties.getThreadsCount());

        ForkJoinPool pool = new ForkJoinPool(threads);
        CopyOnWriteArrayList<String> partial = new CopyOnWriteArrayList<>();
        AtomicBoolean interrupted = new AtomicBoolean(false);

        RecursiveFileScanTask task = new RecursiveFileScanTask(
                patternMatcher,
                Paths.get(scanRequest.directoryPath()),
                scanRequest.scanFilter(),
                scanRequest.depthLimit(),
                0,
                partial,
                interrupted
        );

        // Create task with caching in the end of it
        ForkJoinTask<List<String>> future = pool.submit(() -> {
            List<String> result = task.invoke();
            // Use another instance of cache to avoid dependence on external
            Cache inTaskCache = cacheManager.getCache("fileScanCache");
            if(inTaskCache != null && !cacheSizeEvaluator.isTooBig(result)) {
                String key = scanRequest.directoryPath() + ":" + scanRequest.depthLimit() + ":" + scanRequest.scanFilter();
                inTaskCache.put(key, result);
                // TODO: Cache log here
            }
            return result;
        });
        scans.put(id, new FileScanContext(pool, future, partial, interrupted, cacheKey));
        return new FileScanResult(id, false, List.of());
    }

    public FileScanResult getResult(String token) {
        FileScanContext context = scans.get(token);
        if (context != null) {
            ForkJoinTask<List<String>> future = context.future();

            if (future.isDone()) {
                List<String> full;
                try {
                    full = future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Error getting result", e);
                } finally {
                    cleanup(token);
                }

                // Return full result
                return new FileScanResult(token, true, full);
            } else {
                // Return partial result if scan is not done
                return new FileScanResult(token, false, context.partial());
            }
        }

        // If context not found
        // (it can be case when scan result is cached and startScan() returned result with null token)
        return null;
    }

    /**
     * @return {@code true} if scan found by token and successfully stopped.
     */
    public boolean kill(String token) {
        FileScanContext context = scans.remove(token);
        if (context == null) {
            return false;
        }

        context.interrupted().set(true);
        context.future().cancel(true);
        try(ForkJoinPool pool = context.pool()) {
            pool.shutdownNow();
        }

        return true;
    }

    private void cleanup(String token) {
        FileScanContext context = scans.remove(token);
        if (context != null) {
            // Normal shutdown
            try(ForkJoinPool pool = context.pool()) {
                pool.shutdown();
                try {
                    if (!pool.awaitTermination(NORMAL_SHUTDOWN_TIME_LIMIT_SECS, TimeUnit.SECONDS)) {
                        // Force stop if it takes more time than limit
                        pool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    pool.shutdownNow();
                }
            }
        }
    }

    @PreDestroy
    public void destroyAll() {
        // Correct shutdown when application closes
        scans.forEach((_, context) -> {
            context.future().cancel(true);
            try(ForkJoinPool pool = context.pool()) {
                pool.shutdownNow();
            }
        });
        scans.clear();
    }
}
