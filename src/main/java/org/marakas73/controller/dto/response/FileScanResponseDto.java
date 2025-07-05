package org.marakas73.controller.dto.response;

import java.util.List;

public record FileScanResponseDto(
        List<String> scannedFilePaths
) {
}
