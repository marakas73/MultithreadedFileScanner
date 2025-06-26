package org.marakas73.controller.dto.response;

import java.util.Map;

public class ResponseWrapper<T> {
    public final ResponseStatus status;
    public final Map<String, String> errors;
    public final T data;

    public ResponseWrapper (ResponseStatus status, Map<String, String> errors, T data) {
        this.status = status;
        this.errors = errors;
        this.data = data;
    }
}
