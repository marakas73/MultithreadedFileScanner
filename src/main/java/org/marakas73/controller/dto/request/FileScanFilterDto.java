package org.marakas73.controller.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import org.marakas73.common.IntervalWrapper;

public record FileScanFilterDto(
        @Nullable
        @Size(min = 1, message = "Pattern cannot be empty")
        String namePattern,

        @Nullable
        IntervalWrapper<Long> sizeInBytesInterval,

        @Nullable
        IntervalWrapper<String> lastModifiedDateInterval,

        @Nullable
        IntervalWrapper<String> lastModifiedTimeInterval
) {
}
