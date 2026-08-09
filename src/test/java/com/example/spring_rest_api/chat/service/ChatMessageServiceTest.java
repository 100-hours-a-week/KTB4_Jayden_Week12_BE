package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.service.response.ChatMessagesResponse;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import com.example.spring_rest_api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.example.spring_rest_api.chat.ChatTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @InjectMocks
    ChatMessageService chatMessageService;

    @Mock
    ChatMessageRepository messageRepository;

    @Mock
    ChatRoomAuthorizationService authorizationService;

    @Test
    @DisplayName("GET 메시지 조회는 권한 검증 후 최신순 결과를 오래된순으로 반환한다")
    void getMessagesReturnsOldestFirstAfterAuthorization() {
        ChatRoom room = room(10L);
        User sender = user(2L);
        ChatMessage older = message(100L, room, sender);
        ChatMessage newer = message(101L, room, sender);
        given(messageRepository.findAllInfiniteScroll(10L, null, 20))
                .willReturn(List.of(newer, older));

        List<ChatMessagesResponse> responses =
                chatMessageService.readMessagesInfiniteScroll(1L, 10L, null, 20);

        assertThat(responses).extracting(ChatMessagesResponse::getMessageId)
                .containsExactly(100L, 101L);
        var inOrder = inOrder(authorizationService, messageRepository);
        inOrder.verify(authorizationService).validateParticipant(10L, 1L);
        inOrder.verify(messageRepository).findAllInfiniteScroll(10L, null, 20);
    }

    @Test
    @DisplayName("GET 메시지 조회 권한이 없으면 저장소를 호출하지 않는다")
    void getMessagesWithoutAuthorizationDoesNotQueryRepository() {
        doThrow(new ForbiddenException("denied"))
                .when(authorizationService).validateParticipant(10L, 1L);

        assertThatThrownBy(() ->
                chatMessageService.readMessagesInfiniteScroll(1L, 10L, null, 20))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("GET 전체 읽지 않은 메시지 수는 사용자 ID로 저장소에 위임한다")
    void getUnreadCountDelegatesToRepository() {
        given(messageRepository.countUnreadAll(1L)).willReturn(7L);

        assertThat(chatMessageService.countUnreadMessages(1L)).isEqualTo(7L);
        verify(messageRepository).countUnreadAll(1L);
    }
}
