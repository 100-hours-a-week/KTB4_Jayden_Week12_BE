package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.dto.ClaimedOutbox;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import com.example.spring_rest_api.chat.outbox.repository.OutboxJdbcRepository;
import com.example.spring_rest_api.common.exception.RequestConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxClaimServiceTest {

    @InjectMocks
    OutboxClaimService claimService;

    @Mock
    OutboxJdbcRepository outboxRepository;

    @Test
    @DisplayName("선점 가능한 Outbox가 없으면 상태를 갱신하지 않는다")
    void claimEmptyBatchDoesNotUpdateState() {
        LocalDateTime staleLockTime = LocalDateTime.now().minusMinutes(2);
        given(outboxRepository.findClaimable(staleLockTime, 50)).willReturn(List.of());

        assertThat(claimService.claim("worker-1", 50, staleLockTime)).isEmpty();
        verify(outboxRepository, never()).updateToProcessing(any(), any(), any());
    }

    @Test
    @DisplayName("선점 대상은 같은 worker와 잠금 시각으로 PROCESSING 상태가 된다")
    void claimBatchMarksEveryItemProcessing() {
        LocalDateTime staleLockTime = LocalDateTime.now().minusMinutes(2);
        ClaimedOutbox first = outbox(1L);
        ClaimedOutbox second = outbox(2L);
        given(outboxRepository.findClaimable(staleLockTime, 50)).willReturn(List.of(first, second));
        given(outboxRepository.updateToProcessing(any(), eq("worker-1"), any()))
                .willReturn(1);
        LocalDateTime before = LocalDateTime.now();

        List<ClaimedOutbox> claimed = claimService.claim("worker-1", 50, staleLockTime);

        LocalDateTime after = LocalDateTime.now();
        assertThat(claimed).containsExactly(first, second);
        ArgumentCaptor<LocalDateTime> lockedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxRepository).updateToProcessing(eq(1L), eq("worker-1"), lockedAtCaptor.capture());
        verify(outboxRepository).updateToProcessing(eq(2L), eq("worker-1"), lockedAtCaptor.capture());
        assertThat(lockedAtCaptor.getAllValues()).hasSize(2).allMatch(value ->
                !value.isBefore(before) && !value.isAfter(after));
        assertThat(lockedAtCaptor.getAllValues().get(0))
                .isEqualTo(lockedAtCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("Outbox 상태 갱신 행 수가 1이 아니면 선점 충돌로 처리한다")
    void claimConflictStopsRemainingUpdates() {
        LocalDateTime staleLockTime = LocalDateTime.now().minusMinutes(2);
        given(outboxRepository.findClaimable(staleLockTime, 50))
                .willReturn(List.of(outbox(1L), outbox(2L)));
        given(outboxRepository.updateToProcessing(eq(1L), eq("worker-1"), any()))
                .willReturn(0);

        assertThatThrownBy(() -> claimService.claim("worker-1", 50, staleLockTime))
                .isInstanceOf(RequestConflictException.class);
        verify(outboxRepository, never()).updateToProcessing(eq(2L), any(), any());
    }

    private ClaimedOutbox outbox(Long id) {
        return new ClaimedOutbox(
                id,
                "event-" + id,
                EventType.CHAT_MESSAGE_CREATED,
                "chat.message.v1",
                "payload-" + id
        );
    }
}
