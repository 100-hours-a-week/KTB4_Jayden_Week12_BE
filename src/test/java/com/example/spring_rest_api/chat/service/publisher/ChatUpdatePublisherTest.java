package com.example.spring_rest_api.chat.service.publisher;

import com.example.spring_rest_api.chat.entity.ChatType;
import com.example.spring_rest_api.chat.event.UserChatUpdateEvent;
import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import com.example.spring_rest_api.chat.service.response.ChatUpdateType;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatUpdatePublisherTest {

    @InjectMocks
    ChatUpdatePublisher publisher;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 채팅 업데이트는 전용 Redis 채널에 한 번 발행된다")
    void publishSendsSerializedUpdateToUserChannel() throws Exception {
        ChatRoomUpdateResponse response = response();
        given(objectMapper.writeValueAsString(any(UserChatUpdateEvent.class)))
                .willReturn("payload");

        publisher.publish(1L, response);

        ArgumentCaptor<UserChatUpdateEvent> eventCaptor =
                ArgumentCaptor.forClass(UserChatUpdateEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().getUpdate()).isSameAs(response);
        verify(redisTemplate).convertAndSend("chat.user-update.v1", "payload");
    }

    @Test
    @DisplayName("사용자 채팅 업데이트 직렬화가 실패하면 Redis를 호출하지 않는다")
    void publishSerializationFailureDoesNotCallRedis() throws Exception {
        given(objectMapper.writeValueAsString(any(UserChatUpdateEvent.class)))
                .willThrow(new JsonProcessingException("failed") { });

        assertThatThrownBy(() -> publisher.publish(1L, response()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("JSON_PUBLISH_FAILED");
        verifyNoInteractions(redisTemplate);
    }

    private ChatRoomUpdateResponse response() {
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
