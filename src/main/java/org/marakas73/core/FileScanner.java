package org.marakas73.core;

import org.marakas73.config.FileScannerProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Component
public class FileScanner {
    private final FileNamePatternMatcher patternMatcher;
    private final FileScannerProperties properties;

    public FileScanner(FileNamePatternMatcher patternMatcher, FileScannerProperties properties) {
        this.patternMatcher = patternMatcher;
        this.properties = properties;
    }

    public List<Path> scan(Path path, String pattern) {
        try(var pool = new ForkJoinPool(properties.getThreadsCount())) {
            var fileScanTask = new RecursiveFileScanTask(patternMatcher, path, pattern);
            return pool.invoke(fileScanTask);
        }
    }
}
