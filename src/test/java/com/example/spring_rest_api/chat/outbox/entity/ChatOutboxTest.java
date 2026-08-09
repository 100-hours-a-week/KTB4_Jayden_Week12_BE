package com.example.spring_rest_api.chat.outbox.entity;

import com.example.spring_rest_api.chat.outbox.entity.ChatOutbox;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import com.example.spring_rest_api.chat.outbox.entity.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatOutboxTest {

    @Test
    @DisplayName("신규 Outbox는 재시도 가능한 PENDING 상태로 초기화된다")
    void pendingCreatesRetryableInitialState() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 7, 10, 0);

        ChatOutbox outbox = ChatOutbox.pending(
                "event-1",
                EventType.CHAT_MESSAGE_CREATED,
                "CHAT_MESSAGE",
                100L,
                100L,
                "client-100",
                "chat.message.v1",
                "{\"eventId\":\"event-1\"}",
                now
        );

        assertThat(outbox.getEventId()).isEqualTo("event-1");
        assertThat(outbox.getEventType()).isEqualTo(EventType.CHAT_MESSAGE_CREATED);
        assertThat(outbox.getAggregateType()).isEqualTo("CHAT_MESSAGE");
        assertThat(outbox.getAggregateId()).isEqualTo(100L);
        assertThat(outbox.getMessageId()).isEqualTo(100L);
        assertThat(outbox.getClientMessageId()).isEqualTo("client-100");
        assertThat(outbox.getChannel()).isEqualTo("chat.message.v1");
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getAttemptCount()).isZero();
        assertThat(outbox.getNextRetryAt()).isEqualTo(now);
        assertThat(outbox.getCreatedAt()).isEqualTo(now);
        assertThat(outbox.getLockedBy()).isNull();
        assertThat(outbox.getLockedAt()).isNull();
        assertThat(outbox.getPublishedAt()).isNull();
        assertThat(outbox.getLastError()).isNull();
    }
}
