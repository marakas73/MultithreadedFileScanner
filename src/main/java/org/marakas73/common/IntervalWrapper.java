package org.marakas73.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public class IntervalWrapper<T> {
    private final T start;
    private final T end;

    public IntervalWrapper(
            @JsonProperty("start") @Nullable T start,
            @JsonProperty("end") @Nullable T end) {
        this.start = start;
        this.end = end;
    }

    public T getStart() {
        return start;
    }

    public T getEnd() {
        return end;
    }
}
