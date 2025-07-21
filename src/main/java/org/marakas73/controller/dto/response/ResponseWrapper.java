package org.marakas73.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "API response wrapper")
public class ResponseWrapper<T> {

    @Schema(description = "Show status of request")
    public final ResponseStatus status;

    @Schema(description = "Errors with name and message")
    public final Map<String, String> errors;

    @Schema(description = "Result field")
    public final T data;

    public ResponseWrapper (ResponseStatus status, Map<String, String> errors, T data) {
        this.status = status;
        this.errors = errors;
        this.data = data;
    }
}
