package org.marakas73.service.filescanner;

import jakarta.annotation.PreDestroy;
import org.marakas73.config.FileScannerProperties;
import org.marakas73.model.FileScanContext;
import org.marakas73.model.FileScanRequest;
import org.marakas73.model.FileScanResult;
import org.marakas73.service.filescanner.util.FileScanCacheUtils;
import org.marakas73.service.filtermatcher.FileScanFilterMatcher;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FileScanner {
    private static final int NORMAL_SHUTDOWN_TIME_LIMIT_SECS = 5; // 5 secs
    private static final String FINAL_RESULTS_CACHE_NAME = "fileScanCache";
    private static final String TOKEN_CACHE_NAME = "fileScanTokenCache";

    private final FileScanFilterMatcher patternMatcher;
    private final FileScannerProperties properties;
    private final FileScanCacheUtils cacheUtils;

    private final ConcurrentMap<String, FileScanContext> scans = new ConcurrentHashMap<>();

    public FileScanner(
            FileScanFilterMatcher patternMatcher,
            FileScannerProperties properties,
            FileScanCacheUtils cacheUtils
    ) {
        this.patternMatcher = patternMatcher;
        this.properties = properties;
        this.cacheUtils = cacheUtils;
    }

    public FileScanResult startScan(FileScanRequest scanRequest) {
        String cacheKey = cacheUtils.buildScanCacheKey(scanRequest);
        String token = UUID.randomUUID().toString();

        // Check if result is already cached
        Optional<FileScanResult> resultOptional = cacheUtils.getCachedResult(cacheKey);
        if(resultOptional.isPresent()) {
            // Return cached result
            return resultOptional.orElseThrow(() -> new IllegalStateException("Result is expected but not found"));
        }

        // No cache found by key
        // Starting scanner task
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
            // Caching only full results
            if(!task.isInterrupted()) {
                // Try to put result in cache
                boolean cached = cacheUtils.putValueInCache(cacheKey, result, FINAL_RESULTS_CACHE_NAME);
                if(cached) {
                    // Remove result from buffer if it successfully cached
                    scans.remove(token);
                }
            }
            return result;
        });
        scans.put(token, new FileScanContext(pool, future, partial, interrupted, cacheKey));
        return new FileScanResult(token, false, List.of());
    }

    public FileScanResult getResult(String token) {
        // Get cache key from token caches
        Optional<Object> cacheKeyOptional = cacheUtils.getCachedResult(token, TOKEN_CACHE_NAME);
        if(cacheKeyOptional.isPresent()) {
            String cacheKey = (String) cacheKeyOptional.orElseThrow(
                    () -> new IllegalStateException("Cache key is expected but not found")
            );

            // Check if result is already cached
            Optional<Object> resultOptional = cacheUtils.getCachedResult(cacheKey, FINAL_RESULTS_CACHE_NAME);
            if(resultOptional.isPresent()) {
                // Get result
                List<String> result = (List<String>) resultOptional.orElseThrow(() -> new IllegalStateException("Result is expected but not found"));
                // Return cached file scan result
                FileScanResult fileScanResult = new FileScanResult(
                        null,
                        true,

                );
                return
            }
        }

        // No cached result
        // Search for partial or full result in buffered scans
        FileScanContext context = scans.get(token);
        if (context != null) {
            ForkJoinTask<List<String>> future = context.future();

            if (future.isDone()) {
                List<String> full;
                try {
                    full = future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Error while getting result", e);
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

        // If no scan result found in cache or scans by token
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
