package com.example.spring_rest_api.chat.service.subscriber;

import com.example.spring_rest_api.chat.entity.ChatType;
import com.example.spring_rest_api.chat.event.UserChatUpdateEvent;
import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import com.example.spring_rest_api.chat.service.response.ChatUpdateType;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.example.spring_rest_api.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatUpdateRedisSubscriberTest {

    @InjectMocks
    ChatUpdateRedisSubscriber subscriber;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    Message redisMessage;

    @Test
    @DisplayName("Redis 사용자 업데이트는 대상 사용자의 전용 STOMP queue로 전달된다")
    void onMessageForwardsUpdateToUserQueue() throws Exception {
        ChatRoomUpdateResponse update = update();
        UserChatUpdateEvent event = UserChatUpdateEvent.of(1L, update);
        given(redisMessage.getBody()).willReturn("payload".getBytes(StandardCharsets.UTF_8));
        given(objectMapper.readValue("payload", UserChatUpdateEvent.class)).willReturn(event);

        subscriber.onMessage(redisMessage, null);

        ArgumentCaptor<Object> responseCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("1"),
                eq("/queue/chat-updates"),
                responseCaptor.capture()
        );
        assertThat(responseCaptor.getValue()).isInstanceOfSatisfying(ApiResponse.class, response -> {
            assertThat(response.getMessage()).isEqualTo("chat_room_updated");
            assertThat(response.getData()).isSameAs(update);
        });
    }

    @Test
    @DisplayName("Redis 사용자 업데이트 역직렬화가 실패하면 STOMP로 전달하지 않는다")
    void onMessageDeserializationFailureDoesNotSendStompMessage() throws Exception {
        given(redisMessage.getBody()).willReturn("invalid".getBytes(StandardCharsets.UTF_8));
        given(objectMapper.readValue("invalid", UserChatUpdateEvent.class))
                .willThrow(new RuntimeException("invalid json"));

        assertThatThrownBy(() -> subscriber.onMessage(redisMessage, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("JSON_SUBSCRIBE_FAILED");
        verifyNoInteractions(messagingTemplate);
    }

    private ChatRoomUpdateResponse update() {
        return new ChatRoomUpdateResponse(
                ChatUpdateType.MESSAGE_RECEIVED,
                "client-1",
                10L,
                100L,
                2L,
                "hello",
                ChatType.TEXT,
                LocalDateTime.now(),
                1L
        );
    }
}
