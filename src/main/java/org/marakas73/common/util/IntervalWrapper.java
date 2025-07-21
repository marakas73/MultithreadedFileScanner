package org.marakas73.common.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.io.Serializable;


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@Schema(description = "Interval with start and end values")
public class IntervalWrapper<T> implements Serializable {

    @Schema(description = "Start of the interval")
    private final T start;

    @Schema(description = "End of the interval")
    private final T end;

    @JsonCreator
    public IntervalWrapper(
            @JsonProperty("start") @Nullable T start,
            @JsonProperty("end") @Nullable T end
    ) {
        this.start = start;
        this.end = end;
    }

    @JsonProperty("start")
    public T getStart() {
        return start;
    }

    @JsonProperty("end")
    public T getEnd() {
        return end;
    }
}
