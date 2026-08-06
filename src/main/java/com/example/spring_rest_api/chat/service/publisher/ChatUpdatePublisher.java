package com.example.spring_rest_api.chat.service.publisher;

import com.example.spring_rest_api.chat.event.UserChatUpdateEvent;
import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatUpdatePublisher {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL = "chat.user-update.v1";

    public void publish(Long userId, ChatRoomUpdateResponse response) {
        UserChatUpdateEvent event = UserChatUpdateEvent.of(userId, response);

        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, payload);

        } catch (JsonProcessingException e) {
            throw new BadRequestException("JSON_PUBLISH_FAILED");
        }
    }

}
