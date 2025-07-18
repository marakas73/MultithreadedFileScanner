package org.marakas73.service.filescanner;

import org.marakas73.model.FileScanFilter;
import org.marakas73.service.filtermatcher.FileScanFilterMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecursiveFileScanTask extends RecursiveTask<List<String>> {
    private static final Logger log = LoggerFactory.getLogger(RecursiveFileScanTask.class);

    private final FileScanFilterMatcher fileScanFilterMatcher;
    private final Path targetPath;
    private final FileScanFilter scanFilter;
    private final int depthLimit;
    private final int currentDepth;

    private final CopyOnWriteArrayList<String> partial;
    private final AtomicBoolean interrupted;

    public RecursiveFileScanTask(
            FileScanFilterMatcher fileScanFilterMatcher,
            Path targetPath,
            FileScanFilter scanFilter,
            Integer depthLimit,
            int currentDepth,
            CopyOnWriteArrayList<String> partial,
            AtomicBoolean interrupted
    ) {
        this.fileScanFilterMatcher = fileScanFilterMatcher;
        this.targetPath = targetPath;
        this.scanFilter = scanFilter;
        this.depthLimit = depthLimit != null ? depthLimit : -1; // -1 is default value for unlimited depth
        this.currentDepth = currentDepth;
        this.partial = partial;
        this.interrupted = interrupted;
    }

    public boolean isInterrupted() {
        return interrupted.get();
    }

    @Override
    protected List<String> compute() {
        // Check interrupted flag
        if (interrupted.get() || Thread.currentThread().isInterrupted()) {
            return Collections.emptyList();
        }

        List<RecursiveFileScanTask> subTasks = new ArrayList<>();
        List<String> localResults = new ArrayList<>();

        // Safe directory scanning
        try (var stream = Files.list(targetPath)) {
            for (Path member : stream.toList()) {
                // Each iteration check interrupted flag
                if (interrupted.get() || Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (Files.isRegularFile(member)) {
                    if (fileScanFilterMatcher.matches(member.toAbsolutePath(), scanFilter)) {
                        String pathStr = member.toAbsolutePath().toString();
                        localResults.add(pathStr);
                        partial.add(pathStr);
                    }
                } else if (Files.isDirectory(member)) {
                    // depthLimit == -1 means that no depth limit provided
                    if(depthLimit != -1 && currentDepth > depthLimit) {
                        continue;
                    }

                    RecursiveFileScanTask subTask = new RecursiveFileScanTask(
                            fileScanFilterMatcher,
                            member.toAbsolutePath(),
                            scanFilter,
                            depthLimit,
                            currentDepth + 1,
                            partial,
                            interrupted
                    );
                    subTasks.add(subTask);
                    subTask.fork();
                }
            }

            // Collect subtask results
            subTasks.forEach(subTask -> localResults.addAll(subTask.join()));
        } catch (IOException ioe) {
            log.error("IO Error while file scan {}:", targetPath, ioe);
            // Ignore any IO errors while scanning
        } catch (Exception e) {
            log.error("Error while file scan {}:", targetPath, e);
            // Throw other unexpected error further
            throw e;
        }

        return localResults;
    }
}
