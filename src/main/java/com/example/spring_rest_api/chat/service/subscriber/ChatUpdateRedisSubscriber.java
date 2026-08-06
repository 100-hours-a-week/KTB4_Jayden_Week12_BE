package com.example.spring_rest_api.chat.service.subscriber;

import com.example.spring_rest_api.chat.event.UserChatUpdateEvent;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.example.spring_rest_api.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ChatUpdateRedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String DESTINATION = "/queue/chat-updates";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            UserChatUpdateEvent event = objectMapper.readValue(payload, UserChatUpdateEvent.class);

            messagingTemplate.convertAndSendToUser(
                    event.getUserId().toString(),
                    DESTINATION,
                    ApiResponse.of(
                            "chat_room_updated",
                            event.getUpdate()
                    )
            );
        }  catch (Exception e) {
            throw new BadRequestException("JSON_SUBSCRIBE_FAILED");
        }



    }
}
