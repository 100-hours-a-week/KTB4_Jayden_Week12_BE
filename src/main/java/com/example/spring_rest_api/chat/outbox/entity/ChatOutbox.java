package com.example.spring_rest_api.chat.outbox.entity;

import com.example.spring_rest_api.common.exception.BadRequestException;
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

    public void markProcessing(String workerId, LocalDateTime lockedAt) {
        if (!isPublishable()) {
            throw new BadRequestException("발행할 수 없는 Outbox 상태입니다: " + status);
        }
        this.status = OutboxStatus.PROCESSING;
        this.lockedBy = workerId;
        this.lockedAt = lockedAt;
    }

    public void markPublished(int attemptUsed, LocalDateTime publishedAt) {
        validateProcessing();
        this.status = OutboxStatus.PUBLISHED;
        this.attemptCount += attemptUsed;
        this.publishedAt = publishedAt;

        clearLock();
        this.lastError = null;
    }

    public void markFailed(int attemptUsed, LocalDateTime nextRetryAt, String lastError) {
        validateProcessing();

        this.status = OutboxStatus.FAILED;
        this.attemptCount += attemptUsed;
        this.nextRetryAt = nextRetryAt;
        this.lastError = truncate(lastError, 1000);

        clearLock();
    }

    public void markDead(int attemptUsed, String lastError) {
        validateProcessing();
        this.status = OutboxStatus.DEAD;
        this.attemptCount += attemptUsed;
        this.lastError = truncate(lastError, 1000);

        clearLock();
    }

    public boolean isPublishable() {
        return status == OutboxStatus.PENDING
                || status == OutboxStatus.FAILED
                || status == OutboxStatus.PROCESSING;
    }




    private void validateProcessing() {
        if (status != OutboxStatus.PROCESSING) {
            throw new BadRequestException("PROCESSING 상태에서만 완료할 수 있습니다: " + status);
        }
    }

    private void clearLock() {
        this.lockedBy = null;
        this.lockedAt = null;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }


}
