package org.marakas73.dto;

import java.nio.file.Path;
import java.util.List;

public record FileScanResponseDto(
        ResponseStatus status,
        List<Path> scannedFilePaths
) {
}
