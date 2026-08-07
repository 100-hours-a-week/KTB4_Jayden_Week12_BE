package com.example.spring_rest_api.chat.outbox.repository;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxJdbcRepositoryImpl implements OutboxJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ClaimedOutbox> findClaimable(LocalDateTime staleLockTime, int batchSize) {
        return jdbcTemplate.query(
                """
                        SELECT
                            outbox_id,
                            event_id,
                            event_type,
                            channel,
                            payload
                        FROM chat_outbox
                        WHERE (
                            status IN ('PENDING', 'FAILED')
                            AND next_retry_at <= CURRENT_TIMESTAMP(6)
                        )
                        OR (
                            status = 'PROCESSING'
                            AND locked_at < ?
                        )
                        ORDER BY outbox_id
                        LIMIT ?
                        FOR UPDATE SKIP LOCKED
                        """,
                (resultSet, rowNumber) -> new ClaimedOutbox(
                        resultSet.getLong("outbox_id"),
                        resultSet.getString("event_id"),
                        EventType.valueOf(resultSet.getString("event_type")),
                        resultSet.getString("channel"),
                        resultSet.getString("payload")
                ),
                staleLockTime,
                batchSize
        );
    }

    @Override
    public int updateToProcessing(Long outboxId, String workerId, LocalDateTime lockedAt) {
        return jdbcTemplate.update(
                """
                        UPDATE chat_outbox
                        SET status = 'PROCESSING',
                            locked_by = ?,
                            locked_at = ?
                        WHERE outbox_id = ?
                        """,
                workerId,
                lockedAt,
                outboxId
        );
    }

    @Override
    public int updateToPublished(Long outboxId, String workerId, int attemptsUsed) {
        return jdbcTemplate.update(
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
    }

    @Override
    public int updateToFailed(Long outboxId, String workerId, int attemptsUsed, int retryDelaySeconds, String errorMessage) {
        return jdbcTemplate.update(
                """
                        UPDATE chat_outbox
                        SET status = 'FAILED',
                            attempt_count = attempt_count + ?,
                            next_retry_at = TIMESTAMPADD(
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
                errorMessage,
                outboxId,
                workerId
        );
    }
}
