package com.example.spring_rest_api.chat.service.subscriber;

import com.example.spring_rest_api.chat.event.ChatMessageEvent;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ChatMessageRedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_DESTINATION = "/sub/chatrooms/";


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatMessageEvent event = objectMapper.readValue(payload, ChatMessageEvent.class);

            messagingTemplate.convertAndSend(
                    ROOM_DESTINATION + event.getRoomId(),
                    event.getMessage()
            );
        } catch (Exception e) {
            throw new BadRequestException("JSON_SUBSCRIBE_FAILED");
        }

    }
}
