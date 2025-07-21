package org.marakas73.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import org.marakas73.common.util.IntervalWrapper;

@Schema(description = "Object of all scan files filtering params")
public record FileScanFilterDto(

        @Nullable
        @Size(min = 1, message = "Filename pattern cannot be empty")
        @Schema(description = "Filter by filename pattern", example = "log-*.txt")
        String namePattern,

        @Nullable
        @Schema(description = "Filter by file size (in format of interval)")
        IntervalWrapper<Long> sizeInBytesInterval,

        @Nullable
        @Schema(description = "Filter by last modified date (in format of interval)")
        IntervalWrapper<String> lastModifiedDateInterval,

        @Nullable
        @Schema(description = "Filter by last modified time (in format of interval)")
        IntervalWrapper<String> lastModifiedTimeInterval,

        @Nullable
        @Size(min = 1, message = "Text content pattern cannot be empty")
        @Schema(description = "Filter by text file content", example = "Hello world")
        String textContent
) {
}
