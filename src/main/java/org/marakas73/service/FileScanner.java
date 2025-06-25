package org.marakas73.service;

import org.marakas73.config.FileScannerProperties;
import org.marakas73.service.util.FileNamePatternMatcher;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
public class FileScanner {
    private final FileNamePatternMatcher patternMatcher;
    private final FileScannerProperties properties;

    public FileScanner(FileNamePatternMatcher patternMatcher, FileScannerProperties properties) {
        this.patternMatcher = patternMatcher;
        this.properties = properties;
    }

    public List<Path> scan(Path path, String pattern) {
        return scan(path, pattern, properties.getThreadsCount());
    }

    public List<Path> scan(Path path, String pattern, int threadsCount) {
        try(var pool = new ForkJoinPool(threadsCount)) {
            var fileScanTask = new RecursiveFileScanTask(patternMatcher, path, pattern);
            return pool.invoke(fileScanTask);
        }
    }
}
