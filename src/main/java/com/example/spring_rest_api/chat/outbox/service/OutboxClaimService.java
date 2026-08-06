package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import com.example.spring_rest_api.common.exception.RequestConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedOutbox> claim(String workerId, int batchSize, LocalDateTime staleLockTime) {
        List<ClaimedOutbox> claimed = jdbcTemplate.query(
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
        LocalDateTime lockedAt = LocalDateTime.now();

        claimed.forEach(outbox -> {
            int updatedRows = jdbcTemplate.update(
                    """
                            UPDATE chat_outbox
                            SET status = 'PROCESSING',
                                locked_by = ?,
                                locked_at = ?
                            WHERE outbox_id = ?
                            """,
                    workerId,
                    lockedAt,
                    outbox.getOutboxId()
            );

            if (updatedRows != 1) {
                throw new RequestConflictException("Outbox 선점 상태 변경에 실패했습니다: " + outbox.getOutboxId());
            }
        });

        return claimed;
    }

}
