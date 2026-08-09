package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.dto.PublishResult;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRedisPublisherTest {

    @Mock
    StringRedisTemplate redisTemplate;

    OutboxRedisPublisher publisher;

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .noBackoff()
                .build();
        publisher = new OutboxRedisPublisher(redisTemplate, retryTemplate);
    }

    @Test
    @DisplayName("Redis 발행이 첫 시도에 성공하면 시도 횟수 1을 반환한다")
    void publishSucceedsOnFirstAttempt() {
        ClaimedOutbox outbox = outbox();

        PublishResult result = publisher.publish(outbox);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAttemptsUsed()).isEqualTo(1);
        verify(redisTemplate).convertAndSend("chat.message.v1", "payload");
    }

    @Test
    @DisplayName("Redis 발행이 재시도 후 성공하면 실제 시도 횟수를 반환한다")
    void publishReturnsAttemptsAfterRetrySuccess() {
        doThrow(new RuntimeException("temporary"))
                .doReturn(1L)
                .when(redisTemplate).convertAndSend("chat.message.v1", "payload");

        PublishResult result = publisher.publish(outbox());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAttemptsUsed()).isEqualTo(2);
        verify(redisTemplate, times(2)).convertAndSend("chat.message.v1", "payload");
    }

    @Test
    @DisplayName("Redis 발행이 모두 실패하면 예외와 실제 시도 횟수를 반환한다")
    void publishReturnsFailureAfterAllRetries() {
        RuntimeException failure = new RuntimeException("redis down");
        doThrow(failure).when(redisTemplate).convertAndSend("chat.message.v1", "payload");

        PublishResult result = publisher.publish(outbox());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getAttemptsUsed()).isEqualTo(3);
        assertThat(result.getException()).isSameAs(failure);
        verify(redisTemplate, times(3)).convertAndSend("chat.message.v1", "payload");
    }

    private ClaimedOutbox outbox() {
        return new ClaimedOutbox(
                1L,
                "event-1",
                EventType.CHAT_MESSAGE_CREATED,
                "chat.message.v1",
                "payload"
        );
    }
}
