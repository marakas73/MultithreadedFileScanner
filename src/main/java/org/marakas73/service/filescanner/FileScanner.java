package org.marakas73.service.filescanner;

import org.marakas73.config.FileScannerProperties;
import org.marakas73.model.FileScanRequest;
import org.marakas73.service.filtermatcher.FileScanFilterMatcher;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@CacheConfig(cacheNames = "fileScanCache")
public class FileScanner {
    private final FileScanFilterMatcher patternMatcher;
    private final FileScannerProperties properties;

    private ForkJoinPool pool = null;

    public FileScanner(FileScanFilterMatcher patternMatcher, FileScannerProperties properties) {
        this.patternMatcher = patternMatcher;
        this.properties = properties;
    }

    @Cacheable(key = "#scanRequest.directoryPath + ':' + #scanRequest.scanFilter")
    public List<String> scan(FileScanRequest scanRequest) {
        // Get threads count if provided else use number from properties
        int threadsCount = scanRequest.threadsCount() != null ?
                scanRequest.threadsCount() : properties.getThreadsCount();

        this.pool = new ForkJoinPool(threadsCount);
        try {
            var fileScanTask = new RecursiveFileScanTask(
                    patternMatcher,
                    Paths.get(scanRequest.directoryPath()),
                    scanRequest.scanFilter()
            );
            return pool.invoke(fileScanTask);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Threads count number way too big: " + threadsCount);
        } finally {
            // TODO: Need to close pool after use, but another better way
            this.kill();
        }
    }

    public void kill() {
        if(pool == null || pool.isShutdown()) {
            return;
        }

        pool.shutdownNow();
        pool = null;
    }
}
