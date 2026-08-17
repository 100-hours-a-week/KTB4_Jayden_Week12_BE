package com.example.spring_rest_api.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WebSocketErrorEvent {
    private String username;
    private String sessionId;
    private String code;
    private Long roomId;
}
