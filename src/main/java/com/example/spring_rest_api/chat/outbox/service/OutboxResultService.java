package com.example.spring_rest_api.chat.outbox.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxResultService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markPublished(Long outboxId, String workerId, int attemptsUsed) {
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE chat_outbox
                SET status = 'PUBLISHED',
                    attempt_count = attempt_count + ?,
                    published_at = CURRENT_TIMESTAMP(6),
                    locked_by = NULL,
                    locked_at = NULL,
                    last_error = NULL
                WHERE outbox_id = ?
                    AND status = 'PROCESSING'
                    AND locked_by = ?
                """,
                attemptsUsed,
                outboxId,
                workerId
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
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE chat_outbox
                SET status = 'FAILED',
                    attempt_count = attempt_count + ?,
                    next_retry_at =
                        TIMESTAMPADD(
                            SECOND,
                            ?,
                            CURRENT_TIMESTAMP(6)
                        ),
                    locked_by = NULL,
                    locked_at = NULL,
                    last_error = ?
                WHERE outbox_id = ?
                    AND status = 'PROCESSING'
                    AND locked_by = ?
                """,
                attemptsUsed,
                retryDelaySeconds,
                truncate(errorMessage),
                outboxId,
                workerId
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
