package com.arsh.devsync.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        String path,
        Map<String, String> errors
) {
    public static ErrorResponse of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ErrorResponse(
                status,
                error,
                message,
                LocalDateTime.now(),
                path,
                null
        );
    }

    public static ErrorResponse withErrors(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> errors
    ) {
        return new ErrorResponse(
                status,
                error,
                message,
                LocalDateTime.now(),
                path,
                errors
        );
    }
}