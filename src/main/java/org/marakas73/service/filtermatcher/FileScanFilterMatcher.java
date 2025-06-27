package org.marakas73.service.filtermatcher;

import org.marakas73.model.FileScanFilter;
import org.marakas73.service.filtermatcher.util.FileModificationDateTime;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

@Service
public class FileScanFilterMatcher {
    public boolean matches(Path filePath, FileScanFilter filter) throws IOException {
        if(filter == null) {
            return true;
        }

        if(filter.namePattern() != null) {
            String regex = Pattern.quote(filter.namePattern()).replace("*", "\\E.*\\Q");
            if (!filePath.getFileName().toString().matches(regex)) {
                return false;
            }
        }

        File file = null;
        // If file object needed
        if(filter.sizeInBytesInterval() != null) {
            file = filePath.toFile();
        }

        if(filter.sizeInBytesInterval() != null) {
            var interval = filter.sizeInBytesInterval();
            long fileSize = file.length();
            if(interval.getStart() != null && fileSize < interval.getStart())
                return false;
            if(interval.getEnd() != null && fileSize > interval.getEnd())
                return false;
        }

        if(filter.lastModifiedDateInterval() != null) {
            var interval = filter.lastModifiedDateInterval();
            LocalDate lastModifiedDate = FileModificationDateTime.getLastModifiedDate(filePath);
            if(interval.getStart() != null && lastModifiedDate.isBefore(interval.getStart()))
                return false;
            if(interval.getEnd() != null && lastModifiedDate.isAfter(interval.getEnd()))
                return false;
        }

        if(filter.lastModifiedTimeInterval() != null) {
            var interval = filter.lastModifiedTimeInterval();
            LocalTime lastModifiedTime = FileModificationDateTime.getLastModifiedTime(filePath);
            if(interval.getStart() != null && lastModifiedTime.isBefore(interval.getStart()))
                return false;
            if(interval.getEnd() != null && lastModifiedTime.isAfter(interval.getEnd()))
                return false;
        }

        return true;
    }
}
