package org.marakas73.model;

import jakarta.annotation.Nullable;
import org.marakas73.common.IntervalWrapper;

import java.time.LocalDate;
import java.time.LocalTime;

public record FileScanFilter(
        @Nullable String namePattern,
        @Nullable IntervalWrapper<Long> sizeInBytesInterval,
        @Nullable IntervalWrapper<LocalDate> lastModifiedDateInterval,
        @Nullable IntervalWrapper<LocalTime> lastModifiedTimeInterval
) {
}
