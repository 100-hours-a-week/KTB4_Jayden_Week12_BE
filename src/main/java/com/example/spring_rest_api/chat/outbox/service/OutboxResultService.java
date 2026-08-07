package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.repository.OutboxJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxResultService {
    private final OutboxJdbcRepository outboxJdbcRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markPublished(Long outboxId, String workerId, int attemptsUsed) {
        int updatedRows = outboxJdbcRepository.updateToPublished(
                outboxId,
                workerId,
                attemptsUsed
        );

        return updatedRows == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailed(
            Long outboxId,
            String workerId,
            int attemptsUsed,
            int retryDelaySeconds,
            String errorMessage
    ) {
        int updatedRows = outboxJdbcRepository.updateToFailed(
                outboxId,
                workerId,
                attemptsUsed,
                retryDelaySeconds,
                truncate(errorMessage)
        );

        return updatedRows == 1;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
