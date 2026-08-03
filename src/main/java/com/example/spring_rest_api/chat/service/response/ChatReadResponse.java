package com.example.spring_rest_api.chat.service.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReadResponse {
    private String code;
    private Long roomId;
    private Long readerId;
    private Long lastReadMessageId;

    public static ChatReadResponse updated(Long roomId, Long readerId, Long lastReadMessageId) {
        ChatReadResponse response = new ChatReadResponse();
        response.code = "READ_UPDATED";
        response.roomId = roomId;
        response.readerId = readerId;
        response.lastReadMessageId = lastReadMessageId;
        return response;
    }
}
