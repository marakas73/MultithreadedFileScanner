package org.marakas73.service.filescanner;

import jakarta.annotation.PreDestroy;
import org.marakas73.config.FileScannerProperties;
import org.marakas73.model.FileScanContext;
import org.marakas73.model.FileScanRequest;
import org.marakas73.model.FileScanResult;
import org.marakas73.service.filescanner.exception.ActiveScanCountLimitExceededException;
import org.marakas73.service.filescanner.util.FileScanCacheUtils;
import org.marakas73.service.filtermatcher.FileScanFilterMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FileScanner {
    private static final Logger log = LoggerFactory.getLogger(FileScanner.class);

    private static final int NORMAL_SHUTDOWN_TIME_LIMIT_SECS = 5; // 5 secs
    private static final String FULL_RESULT_CACHE_NAME = "fileScanFullResult";
    private static final String INTERRUPTED_RESULT_CACHE_NAME = "fileScanInterruptedResult";
    private static final String TOKEN_TO_KEY_CACHE_NAME = "fileScanTokenToKey";

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
        final String cacheKey = cacheUtils.buildScanCacheKey(scanRequest);

        // Check if result is already cached
        Optional<List<String>> cachedResultOptional = cacheUtils.getCachedResult(cacheKey, FULL_RESULT_CACHE_NAME);
        if(cachedResultOptional.isPresent()) {
            // Return file scan result with cached result
            log.info("Scan task with request {} already cached, returns it's result", scanRequest);
            return new FileScanResult(null, true, cachedResultOptional.get());
        }

        // No cache found by key
        // Check if active scans count already at maximum number
        if(getActiveScanCount() >= properties.getMaxActiveScans()) {
            throw new ActiveScanCountLimitExceededException(
                    "Cannot add new task, limit exceeded: " + properties.getMaxActiveScans()
            );
        }

        // Create scan task
        final String token = UUID.randomUUID().toString();
        int threads = Optional.ofNullable(scanRequest.threadsCount()).orElse(properties.getThreadsCount());

        ForkJoinPool pool = new ForkJoinPool(threads);
        CopyOnWriteArrayList<String> partial = new CopyOnWriteArrayList<>();
        AtomicBoolean interrupted = new AtomicBoolean(false);

        RecursiveFileScanTask scanTask = new RecursiveFileScanTask(
                patternMatcher,
                Paths.get(scanRequest.directoryPath()),
                scanRequest.scanFilter(),
                scanRequest.depthLimit(),
                0,
                partial,
                interrupted
        );

        // Cache token->key for future result retrieving by token
        cacheUtils.putValueInCache(token, cacheKey, TOKEN_TO_KEY_CACHE_NAME);

        // Create & run async task in ForkJoinPool
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            log.info("Scan task with token {} started", token);
            return pool.invoke(scanTask);
        }, pool);

        scans.put(token, new FileScanContext(pool, future, partial, interrupted, cacheKey));

        // Caching and logging after scan task completion
        future.whenComplete((result, throwable) -> {
            // Log completed state differently but save result anyway
            if(throwable != null) {
                if(throwable instanceof CancellationException) {
                    log.info("Scan task with token {} is completed by cancellation", token);
                } else {
                    log.warn("Scan task with token {} is completed by error: ", token, throwable);
                }

                // Use partial as result because when task is completed by cancellation/error then result will be null
                result = partial;
            } else {
                log.info(
                        "Scan task with token {} is {}",
                        token,
                        scanTask.isInterrupted() ? "interrupted" : "fully completed"
                );
            }

            // Try to put result in cache
            // Put in interrupted or final caches, depend on task status or throwable existence
            String cacheName = scanTask.isInterrupted() || throwable != null
                    ? INTERRUPTED_RESULT_CACHE_NAME
                    : FULL_RESULT_CACHE_NAME;
            boolean cached = cacheUtils.putValueInCache(cacheKey, result, cacheName);

            if(cached) {
                // Result is cached successfully, so no need to store in buffer
                cleanup(token);
            } else {
                // Can't cache result, so mark this scan as completed for auto cleanup in future by scheduled method
                scans.get(token).completedNow();
            }
        });

        // Use 'future.isDone()' instead of 'false' and 'partial' instead of 'List.of()'
        // in case when scan task is very fast to complete or already have any results at this moment
        return new FileScanResult(token, future.isDone(), partial);
    }

    public FileScanResult getResult(String token) {
        // Check if result is already cached
        // Get cache key from token caches
        log.debug("Searching scan result by token: {}", token);
        Optional<String> cacheKeyOptional = cacheUtils.getCachedKeyByToken(token, TOKEN_TO_KEY_CACHE_NAME);
        if(cacheKeyOptional.isPresent()) {
            String cacheKey = cacheKeyOptional.get();
            log.debug("Found cache key by token: {}: {}", token, cacheKey);

            // Try to get cached full result
            Optional<List<String>> fullResultOptional = cacheUtils.getCachedResult(
                    cacheKey, FULL_RESULT_CACHE_NAME
            );
            if(fullResultOptional.isPresent()) {
                // Return cached file scan result with full result
                log.debug("Found and return scan cached full result by token: {}", token);
                return new FileScanResult(
                        null,
                        true,
                        fullResultOptional.get()
                );
            }

            // No cached full result
            // Try to get interrupted cache result
            Optional<List<String>> interruptedCacheResult = cacheUtils.getCachedResult(
                    cacheKey, INTERRUPTED_RESULT_CACHE_NAME
            );
            if(interruptedCacheResult.isPresent()) {
                // Return cached file scan result with interrupted result
                log.debug("Found and return scan cached interrupted result by token: {}", token);
                return new FileScanResult(
                        null,
                        true,
                        interruptedCacheResult.get()
                );
            }
        }

        // No cached result at all
        // Search for partial or full/interrupted result (which somehow can't be cached) in buffered scans
        FileScanContext context = scans.get(token);
        if (context != null) {
            log.debug("Found buffered scan context by token: {}", token);
            CompletableFuture<List<String>> future = context.getFuture();

            if (future.isDone()) {
                List<String> completedResult;
                try {
                    completedResult = future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Error while getting result", e);
                } finally {
                    // Don't store uncached results in buffered scans after showing it
                    cleanup(token);
                }

                // Return completed result (can be full or interrupted)
                log.debug("Found and return scan buffered completed result by token: {}", token);
                return new FileScanResult(token, true, completedResult);
            } else {
                // Return partial result if scan is not done
                log.debug("Found and return scan buffered partial result by token: {}", token);
                return new FileScanResult(token, false, context.getPartial());
            }
        }

        // If no scan result found in cache or scans by token
        log.debug("No scan result found by token: {}", token);
        return null;
    }

    /**
     * @return {@code true} if scan found by token and successfully stopped.
     */
    public boolean kill(String token) {
        FileScanContext context = scans.get(token);
        if (context == null) {
            return false;
        }

        context.isInterrupted().set(true);
        var future = context.getFuture();
        if(!future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally()) {
            future.cancel(true);
        }
        try(ForkJoinPool pool = context.getPool()) {
            pool.shutdownNow();
        }

        return true;
    }

    private void cleanup(String token) {
        FileScanContext context = scans.remove(token);
        if (context != null) {
            // Normal shutdown
            try(ForkJoinPool pool = context.getPool()) {
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

    private long getActiveScanCount() {
        return scans.values().stream()
                .filter(context -> !context.getFuture().isDone()) // Is running
                .count();
    }

    // Scheduled cleanup for removing uncached completed scans from concurrent map
    // Schedule rate based on buffered cache ttl, but 4 times smaller for efficiency compromise
    @Scheduled(fixedRateString = "${scanner.buffered-result-ttl}/4", timeUnit = TimeUnit.SECONDS)
    private void scansScheduledCleanup() {
        log.info("Buffered scans cleanup started.");
        List<String> tokensToClear = new ArrayList<>();
        scans.forEach((key, scanContext) -> {
            if (scanContext.getFuture().isDone()
                    && scanContext.getCompletedAtMillis() != 0
                    && (scanContext.getCompletedAtMillis() + properties.getBufferedResultTtl())
                    > System.currentTimeMillis()
            ) {
                // Result is expired by ttl, add to clearing list
                tokensToClear.add(key);
            }
        });

        // Clear expired results
        tokensToClear.forEach(this::cleanup);
        log.info(
                "Buffered scan cleanup completed. {} of {} ({} active) removed.",
                tokensToClear.size(), scans.size(), getActiveScanCount()
        );
    }

    @PreDestroy
    public void destroyAll() {
        // Correct shutdown when application closes
        scans.forEach((_, context) -> {
            context.getFuture().cancel(true);
            try(ForkJoinPool pool = context.getPool()) {
                pool.shutdownNow();
            }
        });
        scans.clear();
    }
}
