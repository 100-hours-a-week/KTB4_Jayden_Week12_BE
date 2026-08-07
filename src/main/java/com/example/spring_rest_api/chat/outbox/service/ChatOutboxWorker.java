package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.dto.PublishResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatOutboxWorker {
    private final OutboxClaimService claimService;
    private final OutboxRedisPublisher redisPublisher;
    private final OutboxResultService resultService;

    @Value("${app.outbox.worker-id:${random.uuid}}")
    private String workerId;

    @Value("${app.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.outbox.lock-timeout-seconds:120}")
    private int lockTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:500}")
    public void publishPendingEvents() {
        LocalDateTime staleLockTime = LocalDateTime.now().minusSeconds(lockTimeoutSeconds);
        List<ClaimedOutbox> claimed = claimService.claim(workerId, batchSize, staleLockTime);

        claimed.forEach(this::publishOne);
    }

    private void publishOne(ClaimedOutbox outbox) {
        PublishResult result = redisPublisher.publish(outbox);

        if (result.isSuccess()) {
            updatePublishMark(outbox, result);
            return;
        }

        updateFailMark(outbox, result);
    }




    private void updatePublishMark(ClaimedOutbox outbox, PublishResult result) {
        boolean updated = resultService.markPublished(outbox.getOutboxId(), workerId, result.getAttemptsUsed());
        if (!updated) {
            log.warn(
                    "Outbox 발행 성공 후 선점권이 만료되었습니다. " +
                            "outboxId = {}, eventId = {}, workerId = {}",
                    outbox.getOutboxId(),
                    outbox.getEventId(),
                    workerId
            );
        }
    }

    private void updateFailMark(ClaimedOutbox outbox, PublishResult result) {
        int retryDelaySeconds = calculateRetryDelay(result.getAttemptsUsed());
        boolean updated = resultService.markFailed(
                outbox.getOutboxId(),
                workerId,
                result.getAttemptsUsed(),
                retryDelaySeconds,
                result.getException().getMessage()
        );

        if (!updated) {
            log.warn(
                    "Outbox 실패 상태 변경 전에 선점권이 만료되었습니다. " +
                            "outboxId = {}, workerId = {}",
                    outbox.getOutboxId(),
                    workerId
            );
        }
    }

    private int calculateRetryDelay(int attemptsUsed) {
        return switch (attemptsUsed) {
            case 1 -> 5;
            case 2 -> 30;
            default -> 120;
        };
    }
}
