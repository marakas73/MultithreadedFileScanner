package org.marakas73.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Stream;

public class RecursiveFileScanTask extends RecursiveTask<List<Path>> {
    private final FileNamePatternMatcher fileNamePatternMatcher;

    private final Path path;
    private final String pattern;

    public RecursiveFileScanTask(
            FileNamePatternMatcher fileNamePatternMatcher,
            Path path,
            String pattern
    ) {
        this.fileNamePatternMatcher = fileNamePatternMatcher;
        this.path = path;
        this.pattern = pattern;
    }

    @Override
    protected List<Path> compute() {
        try(Stream<Path> directoryMembers = Files.list(path)) {
            List<RecursiveFileScanTask> subTasks = new ArrayList<>();
            List<Path> scannedFilePaths = new ArrayList<>();

            directoryMembers.forEach(member -> {
                if (Files.isRegularFile(member)) {
                    if (fileNamePatternMatcher.matches(member.getFileName().toString(), pattern)) {
                        scannedFilePaths.add(member.toAbsolutePath());
                    }
                } else if (Files.isDirectory(member)) {
                    var subTask = new RecursiveFileScanTask(
                            fileNamePatternMatcher,
                            member.toAbsolutePath(),
                            pattern
                    );
                    subTasks.add(subTask);
                    subTask.fork();
                }
            });

            subTasks.forEach(subTask -> scannedFilePaths.addAll(subTask.join()));
            return scannedFilePaths;
        } catch (Exception e) {
            return List.of();
        }
    }
}
