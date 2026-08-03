package com.example.spring_rest_api.chat.publisher;

import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import com.example.spring_rest_api.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatUpdatePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    private static final String CHAT_UPDATE_DESTINATION = "/queue/chat-updates";

    public void publish(Long userId, ChatRoomUpdateResponse response) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                CHAT_UPDATE_DESTINATION,
                ApiResponse.of(
                        "chat_room_updated",
                        response
                )
        );
    }

}
