package com.example.spring_rest_api.chat.outbox.repository;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxJdbcRepository {

    List<ClaimedOutbox> findClaimable(LocalDateTime staleLockTime, int batchSize);

    int updateToProcessing(Long outboxId, String workerId, LocalDateTime lockedAt);

    int updateToPublished(Long outboxId, String workerId, int attemptsUsed);

    int updateToFailed(Long outboxId, String workerId, int attemptsUsed, int retryDelaySeconds, String errorMessage);
}
