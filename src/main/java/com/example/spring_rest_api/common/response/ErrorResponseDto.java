package com.example.spring_rest_api.common.response;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ErrorResponseDto {
    private final String message;
    private final Object data;

    private ErrorResponseDto(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public static ErrorResponseDto of(String message) {
        return new ErrorResponseDto(message, null);
    }

    public static ErrorResponseDto of(String message, Object data) {
        return new ErrorResponseDto(message, data);
    }
}
