package org.marakas73.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record IntervalWrapper<T>(T start, T end) {
    public IntervalWrapper(
            @JsonProperty("start") @Nullable T start,
            @JsonProperty("end") @Nullable T end
    ) {
        this.start = start;
        this.end = end;
    }
}
