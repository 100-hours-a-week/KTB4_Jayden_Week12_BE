package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import com.example.spring_rest_api.chat.outbox.entity.ChatOutbox;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import com.example.spring_rest_api.chat.outbox.entity.OutboxStatus;
import com.example.spring_rest_api.chat.outbox.repository.ChatOutboxRepository;
import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomMemberRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomRepository;
import com.example.spring_rest_api.chat.service.publisher.ChatUpdatePublisher;
import com.example.spring_rest_api.chat.service.request.ChatRequest;
import com.example.spring_rest_api.chat.service.response.ChatReadResponse;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import com.example.spring_rest_api.common.exception.NotFoundException;
import com.example.spring_rest_api.user.entity.User;
import com.example.spring_rest_api.user.repository.UserQueryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.example.spring_rest_api.chat.ChatTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    ChatService chatService;

    @Mock
    ChatMessageRepository messageRepository;

    @Mock
    ChatRoomRepository chatRoomRepository;

    @Mock
    ChatRoomMemberRepository memberRepository;

    @Mock
    UserQueryRepository userRepository;

    @Mock
    ChatRoomAuthorizationService authorizationService;

    @Mock
    ChatUpdatePublisher updatePublisher;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    ChatOutboxRepository outboxRepository;

    @Test
    @DisplayName("신규 메시지는 권한 검증 후 메시지와 Outbox를 각각 한 번 저장한다")
    void sendNewMessageSavesMessageAndOutboxOnce() throws Exception {
        ChatRoom room = room(10L);
        User sender = user(1L);
        ChatRequest request = chatRequest("client-1", "hello");
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(messageRepository
                .findBySender_UserIdAndChatRoom_ChatRoomIdAndClientMessageId(1L, 10L, "client-1"))
                .willReturn(Optional.empty());
        given(userRepository.findByIdWithProfileImage(1L)).willReturn(Optional.of(sender));
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "chatMessageId", 100L);
            return saved;
        });
        given(objectMapper.writeValueAsString(any())).willReturn("payload");

        ChatResponse response = chatService.sendText(1L, 10L, request);

        assertThat(response.getChatMessageId()).isEqualTo(100L);
        assertThat(response.getClientMessageId()).isEqualTo("client-1");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getContent()).isEqualTo("hello");

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getChatRoom()).isSameAs(room);
        assertThat(messageCaptor.getValue().getSender()).isSameAs(sender);

        ArgumentCaptor<ChatOutbox> outboxCaptor = ArgumentCaptor.forClass(ChatOutbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        ChatOutbox outbox = outboxCaptor.getValue();
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getEventType()).isEqualTo(EventType.CHAT_MESSAGE_CREATED);
        assertThat(outbox.getAggregateId()).isEqualTo(100L);
        assertThat(outbox.getMessageId()).isEqualTo(100L);
        assertThat(outbox.getClientMessageId()).isEqualTo("client-1");
        assertThat(outbox.getChannel()).isEqualTo("chat.message.v1");
        assertThat(outbox.getPayload()).isEqualTo("payload");

        var inOrder = inOrder(chatRoomRepository, authorizationService, messageRepository);
        inOrder.verify(chatRoomRepository).findById(10L);
        inOrder.verify(authorizationService).validateParticipant(10L, 1L);
        inOrder.verify(messageRepository)
                .findBySender_UserIdAndChatRoom_ChatRoomIdAndClientMessageId(1L, 10L, "client-1");
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
        verify(outboxRepository, times(1)).save(any(ChatOutbox.class));
    }

    @Test
    @DisplayName("같은 방의 동일 메시지 재전송은 권한 검증 후 기존 메시지만 반환한다")
    void resendSameMessageReturnsExistingMessageWithoutSaving() {
        ChatRoom room = room(10L);
        ChatMessage existing = message(100L, room, user(1L));
        ChatRequest request = chatRequest(existing.getClientMessageId(), existing.getContent());
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(messageRepository
                .findBySender_UserIdAndChatRoom_ChatRoomIdAndClientMessageId(
                        1L,
                        10L,
                        existing.getClientMessageId()
                ))
                .willReturn(Optional.of(existing));

        ChatResponse response = chatService.sendText(1L, 10L, request);

        assertThat(response.getChatMessageId()).isEqualTo(100L);
        var inOrder = inOrder(chatRoomRepository, authorizationService, messageRepository);
        inOrder.verify(chatRoomRepository).findById(10L);
        inOrder.verify(authorizationService).validateParticipant(10L, 1L);
        inOrder.verify(messageRepository)
                .findBySender_UserIdAndChatRoom_ChatRoomIdAndClientMessageId(
                        1L,
                        10L,
                        existing.getClientMessageId()
                );
        verify(messageRepository, never()).save(any());
        verifyNoInteractions(userRepository, objectMapper, outboxRepository);
    }

    @Test
    @DisplayName("다른 방에서 같은 clientMessageId를 사용해도 현재 방 메시지로 새로 저장한다")
    void reuseClientMessageIdInAnotherRoomCreatesNewMessage() throws Exception {
        ChatRoom currentRoom = room(20L);
        User sender = user(1L);
        ChatRequest request = chatRequest("shared-client-id", "hello");
        given(chatRoomRepository.findById(20L)).willReturn(Optional.of(currentRoom));
        given(messageRepository
                .findBySender_UserIdAndChatRoom_ChatRoomIdAndClientMessageId(
                        1L,
                        20L,
                        "shared-client-id"
                ))
                .willReturn(Optional.empty());
        given(userRepository.findByIdWithProfileImage(1L)).willReturn(Optional.of(sender));
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "chatMessageId", 200L);
            return saved;
        });
        given(objectMapper.writeValueAsString(any())).willReturn("payload");

        ChatResponse response = chatService.sendText(1L, 20L, request);

        assertThat(response.getChatMessageId()).isEqualTo(200L);
        verify(messageRepository).save(any(ChatMessage.class));
        verify(outboxRepository).save(any(ChatOutbox.class));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방으로 메시지를 보내면 후속 작업을 수행하지 않는다")
    void sendMessageToMissingRoomDoesNotPerformFollowUpWork() {
        given(chatRoomRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                chatService.sendText(1L, 10L, chatRequest("client-1", "hello")))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(authorizationService, messageRepository, userRepository, objectMapper, outboxRepository);
    }

    @Test
    @DisplayName("채팅방 참여 권한이 없으면 멱등성 조회와 저장을 수행하지 않는다")
    void sendMessageWithoutAuthorizationDoesNotCheckIdempotencyOrSave() {
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room(10L)));
        doThrow(new ForbiddenException("denied"))
                .when(authorizationService).validateParticipant(10L, 1L);

        assertThatThrownBy(() ->
                chatService.sendText(1L, 10L, chatRequest("client-1", "hello")))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(messageRepository, userRepository, objectMapper, outboxRepository);
    }

    @Test
    @DisplayName("발신 사용자가 없으면 메시지와 Outbox를 저장하지 않는다")
    void sendMessageWithMissingSenderDoesNotSave() {
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room(10L)));
        given(messageRepository
                .findBySender_UserIdAndChatRoom_ChatRoomIdAndClientMessageId(1L, 10L, "client-1"))
                .willReturn(Optional.empty());
        given(userRepository.findByIdWithProfileImage(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                chatService.sendText(1L, 10L, chatRequest("client-1", "hello")))
                .isInstanceOf(NotFoundException.class);
        verify(messageRepository, never()).save(any());
        verifyNoInteractions(objectMapper, outboxRepository);
    }

    @Test
    @DisplayName("메시지 이벤트 직렬화가 실패하면 Outbox를 저장하지 않는다")
    void serializationFailureDoesNotSaveOutbox() throws Exception {
        ChatRoom room = room(10L);
        User sender = user(1L);
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(messageRepository
                .findBySender_UserIdAndChatRoom_ChatRoomIdAndClientMessageId(1L, 10L, "client-1"))
                .willReturn(Optional.empty());
        given(userRepository.findByIdWithProfileImage(1L)).willReturn(Optional.of(sender));
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "chatMessageId", 100L);
            return saved;
        });
        given(objectMapper.writeValueAsString(any()))
                .willThrow(new JsonProcessingException("failed") { });

        assertThatThrownBy(() ->
                chatService.sendText(1L, 10L, chatRequest("client-1", "hello")))
                .isInstanceOf(BadRequestException.class);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 업데이트는 활성 멤버에게만 unread count와 함께 발행한다")
    void publishMessageUpdateSendsOnlyToActiveMembers() {
        ChatResponse response = ChatResponse.from(message(100L, room(10L), user(2L)));
        given(memberRepository.findActiveUserIdsByChatRoomId(10L)).willReturn(List.of(1L, 2L));
        given(messageRepository.countUnreadByRoomIdAndUserId(10L, 1L)).willReturn(3L);
        given(messageRepository.countUnreadByRoomIdAndUserId(10L, 2L)).willReturn(0L);

        chatService.publishMessageUpdate(10L, response);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ChatRoomUpdateResponse> updateCaptor =
                ArgumentCaptor.forClass(ChatRoomUpdateResponse.class);
        verify(updatePublisher, times(2)).publish(userIdCaptor.capture(), updateCaptor.capture());
        assertThat(userIdCaptor.getAllValues()).containsExactly(1L, 2L);
        assertThat(updateCaptor.getAllValues())
                .extracting(ChatRoomUpdateResponse::getUnreadCount)
                .containsExactly(3L, 0L);
        assertThat(updateCaptor.getAllValues())
                .extracting(ChatRoomUpdateResponse::getClientMessageId)
                .containsOnly(response.getClientMessageId());
    }

    @Test
    @DisplayName("더 최신인 같은 방 메시지를 읽으면 읽음 포인터와 응답이 갱신된다")
    void markAsReadUpdatesPointerForNewerMessage() {
        ChatRoom room = room(10L);
        ChatRoomMember member = member(1L, room, user(1L));
        ChatMessage message = message(100L, room, user(2L));
        given(memberRepository.findByChatRoomIdAndUserIdForUpdate(10L, 1L))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatMessageIdAndChatRoom_ChatRoomId(100L, 10L))
                .willReturn(Optional.of(message));

        ChatReadResponse response = chatService.markAsRead(1L, 10L, 100L);

        assertThat(member.getLastReadMessage()).isSameAs(message);
        assertThat(response.getRoomId()).isEqualTo(10L);
        assertThat(response.getReaderId()).isEqualTo(1L);
        assertThat(response.getLastReadMessageId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("동일하거나 과거인 메시지를 읽으면 읽음 포인터 응답을 생성하지 않는다")
    void markAsReadDoesNotMovePointerBackward() {
        ChatRoom room = room(10L);
        ChatRoomMember member = member(1L, room, user(1L));
        ChatMessage current = message(100L, room, user(2L));
        member.readThrough(current);
        given(memberRepository.findByChatRoomIdAndUserIdForUpdate(10L, 1L))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatMessageIdAndChatRoom_ChatRoomId(100L, 10L))
                .willReturn(Optional.of(current));

        assertThat(chatService.markAsRead(1L, 10L, 100L)).isNull();
        assertThat(member.getLastReadMessage()).isSameAs(current);
    }

    @Test
    @DisplayName("퇴장한 멤버는 메시지를 읽음 처리할 수 없다")
    void markAsReadForLeftMemberThrowsException() {
        ChatRoomMember member = member(1L, room(10L), user(1L));
        member.leave();
        given(memberRepository.findByChatRoomIdAndUserIdForUpdate(10L, 1L))
                .willReturn(Optional.of(member));

        assertThatThrownBy(() -> chatService.markAsRead(1L, 10L, 100L))
                .isInstanceOf(ForbiddenException.class);
        verify(messageRepository, never())
                .findByChatMessageIdAndChatRoom_ChatRoomId(any(), any());
    }

    @Test
    @DisplayName("요청한 채팅방에 메시지가 없으면 읽음 포인터를 변경하지 않는다")
    void markAsReadForMissingRoomMessageDoesNotChangePointer() {
        ChatRoomMember member = member(1L, room(10L), user(1L));
        given(memberRepository.findByChatRoomIdAndUserIdForUpdate(10L, 1L))
                .willReturn(Optional.of(member));
        given(messageRepository.findByChatMessageIdAndChatRoom_ChatRoomId(100L, 10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.markAsRead(1L, 10L, 100L))
                .isInstanceOf(NotFoundException.class);
        assertThat(member.getLastReadMessage()).isNull();
    }
}
