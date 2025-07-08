package org.marakas73.controller.dto.response;

import java.util.List;

public record FileScanResponseDto(
        String token,
        boolean completed,
        List<String> result
) {
}
