package org.marakas73.service;

import org.marakas73.model.FileScanFilter;
import org.marakas73.service.util.FileScanFilterMatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Stream;

public class RecursiveFileScanTask extends RecursiveTask<List<Path>> {
    private final FileScanFilterMatcher fileScanFilterMatcher;

    private final Path targetPath;
    private final FileScanFilter scanFilter;

    public RecursiveFileScanTask(
            FileScanFilterMatcher fileScanFilterMatcher,
            Path targetPath,
            FileScanFilter scanFilter
    ) {
        this.fileScanFilterMatcher = fileScanFilterMatcher;
        this.targetPath = targetPath;
        this.scanFilter = scanFilter;
    }

    @Override
    protected List<Path> compute() {
        try(Stream<Path> directoryMembers = Files.list(targetPath)) {
            List<RecursiveFileScanTask> subTasks = new ArrayList<>();
            List<Path> scannedFilePaths = new ArrayList<>();

            for(var member : directoryMembers.toList()) {
                if (Files.isRegularFile(member)) {
                    if (fileScanFilterMatcher.matches(member.toAbsolutePath(), scanFilter)) {
                        scannedFilePaths.add(member.toAbsolutePath());
                    }
                } else if (Files.isDirectory(member)) {
                    var subTask = new RecursiveFileScanTask(
                            fileScanFilterMatcher,
                            member.toAbsolutePath(),
                            scanFilter
                    );
                    subTasks.add(subTask);
                    subTask.fork();
                }
            }

            subTasks.forEach(subTask -> scannedFilePaths.addAll(subTask.join()));
            return scannedFilePaths;
        } catch (Exception e) {
            return List.of();
        }
    }
}
