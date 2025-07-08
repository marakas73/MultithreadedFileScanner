package org.marakas73.service.filescanner;

import org.marakas73.model.FileScanFilter;
import org.marakas73.service.filtermatcher.FileScanFilterMatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Stream;

public class RecursiveFileScanTask extends RecursiveTask<List<String>> {
    private final FileScanFilterMatcher fileScanFilterMatcher;

    private final Path targetPath;
    private final FileScanFilter scanFilter;
    private final int depthLimit;
    private final int currentDepth;

    public RecursiveFileScanTask(
            FileScanFilterMatcher fileScanFilterMatcher,
            Path targetPath,
            FileScanFilter scanFilter,
            Integer depthLimit,
            int currentDepth
    ) {
        this.fileScanFilterMatcher = fileScanFilterMatcher;
        this.targetPath = targetPath;
        this.scanFilter = scanFilter;
        this.depthLimit = depthLimit != null ? depthLimit : -1;
        this.currentDepth = currentDepth;
    }

    @Override
    protected List<String> compute() {
        // depthLimit == -1 means that no depth limit provided (recursing depth is unlimited)
        if(depthLimit != -1 && currentDepth > depthLimit) {
            // Exit current scan iteration with empty list because of depth limit exceeded
            return Collections.emptyList();
        }

        try(Stream<Path> directoryMembers = Files.list(targetPath)) {
            List<RecursiveFileScanTask> subTasks = new ArrayList<>();
            List<String> scannedFilePaths = new ArrayList<>();

            for(var member : directoryMembers.toList()) {
                if (Files.isRegularFile(member)) {
                    if (fileScanFilterMatcher.matches(member.toAbsolutePath(), scanFilter)) {
                        scannedFilePaths.add(member.toAbsolutePath().toString());
                    }
                } else if (Files.isDirectory(member)) {
                    var subTask = new RecursiveFileScanTask(
                            fileScanFilterMatcher,
                            member.toAbsolutePath(),
                            scanFilter,
                            depthLimit,
                            currentDepth + 1
                    );
                    subTasks.add(subTask);
                    subTask.fork();
                }
            }

            subTasks.forEach(subTask -> scannedFilePaths.addAll(subTask.join()));
            return scannedFilePaths;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
