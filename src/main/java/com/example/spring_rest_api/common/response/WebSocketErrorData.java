package com.example.spring_rest_api.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WebSocketErrorData {
    private Long roomId;

    public static WebSocketErrorData of(Long roomId) {
        return new WebSocketErrorData(roomId);
    }
}
