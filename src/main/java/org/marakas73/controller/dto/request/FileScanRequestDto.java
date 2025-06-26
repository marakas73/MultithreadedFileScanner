package org.marakas73.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FileScanRequestDto(
        @NotNull(message = "Directory path cannot be null")
        @Size(min = 1, message = "Directory path must not be empty")
        String directoryPath,

        @Positive(message = "Threads count must be positive")
        Integer threadsCount,

        FileScanFilterDto scanFilter
) {
}
