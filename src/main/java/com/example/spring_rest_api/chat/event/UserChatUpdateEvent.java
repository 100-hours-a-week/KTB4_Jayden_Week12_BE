package com.example.spring_rest_api.chat.event;

import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class UserChatUpdateEvent {
    private String eventId;
    private int schemaVersion;
    private Long userId;
    private ChatRoomUpdateResponse update;

    public static UserChatUpdateEvent of( Long userId, ChatRoomUpdateResponse update) {
        UserChatUpdateEvent event = new UserChatUpdateEvent();
        event.eventId = UUID.randomUUID().toString();
        event.schemaVersion = 1;
        event.userId = userId;
        event.update = update;
        return event;
    }
}
