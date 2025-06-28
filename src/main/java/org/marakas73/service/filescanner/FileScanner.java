package org.marakas73.service.filescanner;

import org.marakas73.common.util.SupportedTextFileFormats;
import org.marakas73.config.FileScannerProperties;
import org.marakas73.model.FileScanRequest;
import org.marakas73.service.filescanner.exception.InconsistentFilterException;
import org.marakas73.service.filtermatcher.FileScanFilterMatcher;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
public class FileScanner {
    private final FileScanFilterMatcher patternMatcher;
    private final FileScannerProperties properties;

    public FileScanner(FileScanFilterMatcher patternMatcher, FileScannerProperties properties) {
        this.patternMatcher = patternMatcher;
        this.properties = properties;
    }

    /**
     * @throws InconsistentFilterException if text content pattern filter provided but
     * target file extension in filename pattern is not the text file.
     * @throws IllegalArgumentException if threads count more than allowed in ForkJoinPool.
     */
    public List<Path> scan(FileScanRequest scanRequest) {
        // Get threads count if provided else use number from properties
        int threadsCount = scanRequest.threadsCount() != null ?
                scanRequest.threadsCount() : properties.getThreadsCount();

        // If filter has text content filtering param need to check if name pattern
        if(scanRequest.scanFilter() != null) {
            var filter = scanRequest.scanFilter();
            if(filter.textContent() != null && !SupportedTextFileFormats.isTextFile(filter.namePattern())) {
                throw new InconsistentFilterException("Text content pattern can be applied only for text files");
            }
        }

        try(var pool = new ForkJoinPool(threadsCount)) {
            var fileScanTask = new RecursiveFileScanTask(
                    patternMatcher,
                    Paths.get(scanRequest.directoryPath()),
                    scanRequest.scanFilter()
            );
            return pool.invoke(fileScanTask);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Threads count number way too big: " + threadsCount);
        }
    }
}
