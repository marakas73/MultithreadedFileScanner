package org.marakas73.controller.dto.response;

import java.nio.file.Path;
import java.util.List;

public record FileScanResponseDto(
        List<Path> scannedFilePaths
) {
}
