package com.example.spring_rest_api.chat.outbox.service;

import com.example.spring_rest_api.chat.outbox.repository.OutboxJdbcRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxResultServiceTest {

    @InjectMocks
    OutboxResultService resultService;

    @Mock
    OutboxJdbcRepository outboxRepository;

    @Test
    @DisplayName("발행 완료 갱신 행 수가 1일 때만 성공을 반환한다")
    void markPublishedReturnsTrueOnlyForSingleUpdatedRow() {
        given(outboxRepository.updateToPublished(1L, "worker-1", 1)).willReturn(1);
        given(outboxRepository.updateToPublished(2L, "worker-1", 1)).willReturn(0);

        assertThat(resultService.markPublished(1L, "worker-1", 1)).isTrue();
        assertThat(resultService.markPublished(2L, "worker-1", 1)).isFalse();
    }

    @Test
    @DisplayName("발행 실패 갱신 행 수가 1일 때만 성공을 반환한다")
    void markFailedReturnsTrueOnlyForSingleUpdatedRow() {
        given(outboxRepository.updateToFailed(1L, "worker-1", 2, 30, "error"))
                .willReturn(1);
        given(outboxRepository.updateToFailed(2L, "worker-1", 2, 30, "error"))
                .willReturn(2);

        assertThat(resultService.markFailed(1L, "worker-1", 2, 30, "error")).isTrue();
        assertThat(resultService.markFailed(2L, "worker-1", 2, 30, "error")).isFalse();
    }

    @Test
    @DisplayName("발행 실패 오류 메시지는 1000자로 제한해 저장한다")
    void markFailedTruncatesLongErrorMessage() {
        String longError = "e".repeat(1001);
        given(outboxRepository.updateToFailed(1L, "worker-1", 3, 120, "e".repeat(1000)))
                .willReturn(1);

        assertThat(resultService.markFailed(1L, "worker-1", 3, 120, longError)).isTrue();
        verify(outboxRepository)
                .updateToFailed(1L, "worker-1", 3, 120, "e".repeat(1000));
    }

    @Test
    @DisplayName("발행 실패 오류 메시지가 null이면 그대로 저장한다")
    void markFailedKeepsNullErrorMessage() {
        given(outboxRepository.updateToFailed(1L, "worker-1", 1, 5, null)).willReturn(1);

        assertThat(resultService.markFailed(1L, "worker-1", 1, 5, null)).isTrue();
    }
}
