package com.example.spring_rest_api.chat.outbox.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "chat_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outboxId;
    private String eventId;
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private String aggregateType;
    private Long aggregateId;

    private Long messageId;
    private String clientMessageId;
    private String channel;

    @Lob
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private int attemptCount;
    private LocalDateTime nextRetryAt;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime publishedAt;
    private String lastError;
    private LocalDateTime createdAt;

    public static ChatOutbox pending(
            String eventId,
            EventType eventType,
            String aggregateType,
            Long aggregateId,
            Long messageId,
            String clientMessageId,
            String channel,
            String payload,
            LocalDateTime now
    ) {
        ChatOutbox outbox = new ChatOutbox();
        outbox.eventId = eventId;
        outbox.eventType = eventType;
        outbox.aggregateType = aggregateType;
        outbox.aggregateId = aggregateId;
        outbox.messageId = messageId;
        outbox.clientMessageId = clientMessageId;
        outbox.channel = channel;
        outbox.payload = payload;

        outbox.status = OutboxStatus.PENDING;
        outbox.attemptCount = 0;
        outbox.nextRetryAt = now;

        outbox.lockedBy = null;
        outbox.lockedAt = null;
        outbox.publishedAt = null;
        outbox.lastError = null;
        outbox.createdAt = now;
        return outbox;
    }
}
