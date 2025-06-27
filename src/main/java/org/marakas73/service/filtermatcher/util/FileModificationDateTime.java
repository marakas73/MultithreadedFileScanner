package org.marakas73.service.filtermatcher.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public class FileModificationDateTime {
    public static LocalDate getLastModifiedDate(Path filePath) throws IOException {
        FileTime fileTime = Files.getLastModifiedTime(filePath);
        return fileTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static LocalTime getLastModifiedTime(Path filePath) throws IOException {
        FileTime fileTime = Files.getLastModifiedTime(filePath);
        return fileTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime();
    }
}