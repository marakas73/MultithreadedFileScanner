package org.marakas73.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "File scan creation full request")
public record FileScanRequestDto(

        @NotNull(message = "Directory path cannot be null")
        @Size(min = 1, message = "Directory path must not be empty")
        @Schema(description = "Target path to scan", example = "C:/Users/User/")
        String directoryPath,

        @Positive(message = "Threads count must be positive")
        @Schema(description = "Max threads count to use in scan", minimum = "1")
        Integer threadsCount,

        @Positive(message = "Depth limit must be positive")
        @Schema(description = "Recursive depth limit for scan", minimum = "0")
        Integer depthLimit,

        @Schema(description = "Object of all filtering params")
        FileScanFilterDto scanFilter
) {
}
