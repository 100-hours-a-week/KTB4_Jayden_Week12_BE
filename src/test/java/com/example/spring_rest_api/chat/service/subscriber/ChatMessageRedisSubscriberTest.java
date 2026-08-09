package com.example.spring_rest_api.chat.service.subscriber;

import com.example.spring_rest_api.chat.event.ChatMessageEvent;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

import static com.example.spring_rest_api.chat.ChatTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageRedisSubscriberTest {

    @InjectMocks
    ChatMessageRedisSubscriber subscriber;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    Message redisMessage;

    @Test
    @DisplayName("Redis 채팅 메시지는 해당 채팅방 STOMP destination으로 전달된다")
    void onMessageForwardsChatMessageToRoomDestination() throws Exception {
        ChatResponse response = ChatResponse.from(message(100L, room(10L), user(2L)));
        ChatMessageEvent event = ChatMessageEvent.of(10L, response);
        given(redisMessage.getBody()).willReturn("payload".getBytes(StandardCharsets.UTF_8));
        given(objectMapper.readValue("payload", ChatMessageEvent.class)).willReturn(event);

        subscriber.onMessage(redisMessage, null);

        verify(messagingTemplate).convertAndSend("/sub/chatrooms/10", response);
    }

    @Test
    @DisplayName("Redis 채팅 메시지 역직렬화가 실패하면 STOMP로 전달하지 않는다")
    void onMessageDeserializationFailureDoesNotSendStompMessage() throws Exception {
        given(redisMessage.getBody()).willReturn("invalid".getBytes(StandardCharsets.UTF_8));
        given(objectMapper.readValue("invalid", ChatMessageEvent.class))
                .willThrow(new RuntimeException("invalid json"));

        assertThatThrownBy(() -> subscriber.onMessage(redisMessage, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("JSON_SUBSCRIBE_FAILED");
        verifyNoInteractions(messagingTemplate);
    }
}
