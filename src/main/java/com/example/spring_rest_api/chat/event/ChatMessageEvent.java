package com.example.spring_rest_api.chat.event;

import com.example.spring_rest_api.chat.service.response.ChatResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ChatMessageEvent {
    private String eventId;
    private int schemaVersion;
    private Long roomId;
    private ChatResponse message;

    public static ChatMessageEvent of(Long roomId, ChatResponse message) {
        ChatMessageEvent event = new ChatMessageEvent();
        event.eventId = UUID.randomUUID().toString();
        event.schemaVersion = 1;
        event.roomId = roomId;
        event.message = message;
        return event;
    }

}
