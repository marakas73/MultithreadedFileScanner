package org.marakas73.run;

import org.marakas73.config.FileScannerProperties;
import org.marakas73.core.FileScanner;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

@Component
public class ConsoleFileScannerRunner {
    private final FileScannerProperties properties;
    private final FileScanner fileScanner;

    public ConsoleFileScannerRunner(FileScannerProperties properties, FileScanner fileScanner) {
        this.properties = properties;
        this.fileScanner = fileScanner;
    }

    public void run() {
        Path path;
        String fileNamePattern;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            // Path input
            System.out.print("Enter directory (or 'q' to quit): ");
            String input = scanner.nextLine();
            if ("q".equalsIgnoreCase(input)) {
                scanner.close();
                return;
            }
            try {
                if(input.startsWith("\"") && input.endsWith("\"") && input.length() >= 2) {
                    // Trim "" for path
                    input = input.substring(1, input.length() - 1);
                }
                path = Paths.get(input);
            } catch (InvalidPathException e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }

            // File pattern input
            System.out.print("Enter file pattern (leave empty to scan all files): ");
            fileNamePattern = scanner.nextLine();

            // Threads input
            System.out.print("Enter number of threads (Enter for " + properties.getThreadsCount() + "): ");
            String threadsInput = scanner.nextLine();
            if (!threadsInput.isEmpty()) {
                try {
                    properties.setThreadsCount(Integer.parseInt(threadsInput));
                } catch (IllegalArgumentException e) {
                    System.out.println(
                            "Error: " + e.getMessage() + ". Using default (" + properties.getThreadsCount() + ")."
                    );
                }
            }

            // Run scan
            System.out.println("Scanning...");
            long startMillis = System.currentTimeMillis();

            var scannedFilePaths = fileScanner.scan(path, fileNamePattern);
            scannedFilePaths.forEach(System.out::println);

            System.out.println("Scanned files: " + scannedFilePaths.size());
            System.out.println("Scanned in " + (System.currentTimeMillis() - startMillis) + "ms");
        }
    }
}