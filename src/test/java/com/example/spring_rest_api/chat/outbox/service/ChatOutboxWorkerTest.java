package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.dto.PublishResult;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatOutboxWorkerTest {

    @InjectMocks
    ChatOutboxWorker worker;

    @Mock
    OutboxClaimService claimService;

    @Mock
    OutboxRedisPublisher redisPublisher;

    @Mock
    OutboxResultService resultService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(worker, "workerId", "worker-1");
        ReflectionTestUtils.setField(worker, "batchSize", 50);
        ReflectionTestUtils.setField(worker, "lockTimeoutSeconds", 120);
    }

    @Test
    @DisplayName("선점된 Outbox가 없으면 발행과 결과 저장을 호출하지 않는다")
    void publishPendingEventsWithEmptyBatchDoesNothing() {
        given(claimService.claim(eq("worker-1"), eq(50), any(LocalDateTime.class)))
                .willReturn(List.of());

        worker.publishPendingEvents();

        verifyNoInteractions(redisPublisher, resultService);
    }

    @Test
    @DisplayName("Outbox 발행 성공 시 발행 완료만 한 번 기록한다")
    void publishPendingEventsMarksPublishedOnSuccess() {
        ClaimedOutbox outbox = outbox(1L);
        given(claimService.claim(eq("worker-1"), eq(50), any(LocalDateTime.class)))
                .willReturn(List.of(outbox));
        given(redisPublisher.publish(outbox)).willReturn(PublishResult.success(2));
        given(resultService.markPublished(1L, "worker-1", 2)).willReturn(true);

        worker.publishPendingEvents();

        verify(resultService).markPublished(1L, "worker-1", 2);
        verify(resultService, never()).markFailed(any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Outbox 첫 시도 발행 실패 시 5초 뒤 재시도로 기록한다")
    void publishPendingEventsSchedulesFiveSecondRetryAfterFirstFailure() {
        ClaimedOutbox outbox = outbox(1L);
        RuntimeException failure = new RuntimeException("redis down");
        given(claimService.claim(eq("worker-1"), eq(50), any(LocalDateTime.class)))
                .willReturn(List.of(outbox));
        given(redisPublisher.publish(outbox)).willReturn(PublishResult.failure(1, failure));
        given(resultService.markFailed(1L, "worker-1", 1, 5, "redis down"))
                .willReturn(true);

        worker.publishPendingEvents();

        verify(resultService).markFailed(1L, "worker-1", 1, 5, "redis down");
        verify(resultService, never()).markPublished(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Outbox 두 번째와 세 번째 이상 실패는 각각 30초와 120초 뒤 재시도로 기록한다")
    void publishPendingEventsUsesIncreasingRetryDelay() {
        ClaimedOutbox second = outbox(2L);
        ClaimedOutbox third = outbox(3L);
        RuntimeException failure = new RuntimeException("redis down");
        given(claimService.claim(eq("worker-1"), eq(50), any(LocalDateTime.class)))
                .willReturn(List.of(second, third));
        given(redisPublisher.publish(second)).willReturn(PublishResult.failure(2, failure));
        given(redisPublisher.publish(third)).willReturn(PublishResult.failure(3, failure));

        worker.publishPendingEvents();

        verify(resultService).markFailed(2L, "worker-1", 2, 30, "redis down");
        verify(resultService).markFailed(3L, "worker-1", 3, 120, "redis down");
    }

    private ClaimedOutbox outbox(Long id) {
        return new ClaimedOutbox(
                id,
                "event-" + id,
                EventType.CHAT_MESSAGE_CREATED,
                "chat.message.v1",
                "payload-" + id
        );
    }
}
