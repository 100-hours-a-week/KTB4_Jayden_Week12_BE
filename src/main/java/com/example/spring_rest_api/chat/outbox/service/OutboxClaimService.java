package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.repository.OutboxJdbcRepository;
import com.example.spring_rest_api.common.exception.RequestConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {
    private final OutboxJdbcRepository outboxJdbcRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedOutbox> claim(String workerId, int batchSize, LocalDateTime staleLockTime) {
        List<ClaimedOutbox> claimed = outboxJdbcRepository.findClaimable(staleLockTime, batchSize);
        LocalDateTime lockedAt = LocalDateTime.now();

        claimed.forEach(outbox -> {
            int updatedRows = outboxJdbcRepository.updateToProcessing(
                    outbox.getOutboxId(),
                    workerId,
                    lockedAt
            );

            if (updatedRows != 1) {
                throw new RequestConflictException(
                        "Outbox 선점 상태 변경에 실패했습니다: "
                        + outbox.getOutboxId()
                );
            }
        });

        return claimed;
    }

}
