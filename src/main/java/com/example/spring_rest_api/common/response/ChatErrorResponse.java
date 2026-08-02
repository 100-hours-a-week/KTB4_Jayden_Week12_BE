package com.example.spring_rest_api.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatErrorResponse {
    private String code;
    private String message;
    private Long roomId;

    public static ChatErrorResponse of(String code, String message, Long roomId) {
        return new ChatErrorResponse(code, message, roomId);
    }
}
