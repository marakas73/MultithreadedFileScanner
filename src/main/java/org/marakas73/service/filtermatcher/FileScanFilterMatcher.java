package org.marakas73.service.filtermatcher;

import org.marakas73.common.util.IntervalWrapper;
import org.marakas73.common.util.SupportedTextFileFormats;
import org.marakas73.config.FileScannerProperties;
import org.marakas73.model.FileScanFilter;
import org.marakas73.service.filescanner.exception.InconsistentFilterException;
import org.marakas73.service.filtermatcher.util.FileModificationDateTime;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

@Service
public class FileScanFilterMatcher {
    private final FileScannerProperties properties;

    public FileScanFilterMatcher(FileScannerProperties properties) {
        this.properties = properties;
    }

    public boolean matches(Path filePath, FileScanFilter filter) throws IOException {
        if(filter == null) {
            return true;
        }

        return matchesByFileNamePattern(filePath, filter.namePattern()) &&
                matchesByLastModifiedDateInterval(filePath, filter.lastModifiedDateInterval()) &&
                matchesByLastModifiedTimeInterval(filePath, filter.lastModifiedTimeInterval()) &&
                matchesByFileSizeInterval(filePath, filter.sizeInBytesInterval()) &&
                matchesByTextContentPattern(filePath, filter.textContent());
    }

    private boolean matchesByFileNamePattern(Path filePath, String pattern) {
        if(pattern == null) {
            return true;
        }

        String regex = Pattern.quote(pattern).replace("*", "\\E.*\\Q");
        return filePath.getFileName().toString().matches(regex);
    }

    private boolean matchesByFileSizeInterval(Path filePath, IntervalWrapper<Long> interval) {
        if(interval == null) {
            return true;
        }

        File file = filePath.toFile();
        long fileSize = file.length();
        if(interval.getStart() != null && fileSize < interval.getStart())
            return false;
        return interval.getEnd() == null || fileSize <= interval.getEnd();
    }

    private boolean matchesByLastModifiedDateInterval(
            Path filePath,
            IntervalWrapper<LocalDate> interval
    ) throws IOException {
        if(interval == null) {
            return true;
        }

        LocalDate lastModifiedDate = FileModificationDateTime.getLastModifiedDate(filePath);
        if(interval.getStart() != null && lastModifiedDate.isBefore(interval.getStart()))
            return false;
        return interval.getEnd() == null || !lastModifiedDate.isAfter(interval.getEnd());
    }

    private boolean matchesByLastModifiedTimeInterval(
            Path filePath,
            IntervalWrapper<LocalTime> interval
    ) throws IOException {
        if(interval == null) {
            return true;
        }

        LocalTime lastModifiedTime = FileModificationDateTime.getLastModifiedTime(filePath);
        if(interval.getStart() != null && lastModifiedTime.isBefore(interval.getStart()))
            return false;
        return interval.getEnd() == null || !lastModifiedTime.isAfter(interval.getEnd());
    }

    private boolean matchesByTextContentPattern(Path filePath, String textContent) {
        if (textContent == null) {
            return true;
        }

        if (!SupportedTextFileFormats.isTextFile(filePath.getFileName().toString())) {
            throw new InconsistentFilterException("Text content pattern can be apply only for text files");
        }

        try {
            long fileSize = Files.size(filePath);
            textContent = textContent.toLowerCase();

            // Stream read for small files (lower than stream file size limit)
            if (fileSize <= properties.getStreamFileSizeLimit()) {
                String content = Files.readString(filePath, StandardCharsets.UTF_8).toLowerCase();
                return content.contains(textContent);
            } else {
                // Sequence read for bigger files
                try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains(textContent)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (IOException | OutOfMemoryError e) {
            return false;
        }
    }
}
