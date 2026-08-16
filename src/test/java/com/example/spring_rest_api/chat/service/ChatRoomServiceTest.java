package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomMemberRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomRepository;
import com.example.spring_rest_api.chat.service.request.ChatRoomCreateOrGetRequest;
import com.example.spring_rest_api.chat.service.response.ChatRoomCreateOrGetResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomInfoResponse;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import com.example.spring_rest_api.common.exception.NotFoundException;
import com.example.spring_rest_api.user.entity.User;
import com.example.spring_rest_api.user.repository.UserQueryRepository;
import com.example.spring_rest_api.user.repository.UserRepository;
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
class ChatRoomServiceTest {

    @InjectMocks
    ChatRoomService chatRoomService;

    @Mock
    ChatRoomRepository chatRoomRepository;

    @Mock
    ChatRoomMemberRepository memberRepository;

    @Mock
    ChatMessageRepository messageRepository;

    @Mock
    ChatRoomAuthorizationService authorizationService;

    @Mock
    UserRepository userRepository;

    @Mock
    UserQueryRepository userQueryRepository;

    @Test
    @DisplayName("POST 신규 1대1 채팅방은 정렬된 키로 방과 두 멤버를 생성한다")
    void createDirectRoomSavesRoomAndTwoMembers() {
        User requester = user(9L);
        User opponent = user(2L);
        ChatRoomCreateOrGetRequest request = roomRequest(2L);
        given(userRepository.findById(9L)).willReturn(Optional.of(requester));
        given(userQueryRepository.findByIdWithProfileImage(2L)).willReturn(Optional.of(opponent));
        given(chatRoomRepository.findByDirectKey("2:9")).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
            ChatRoom saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "chatRoomId", 10L);
            return saved;
        });

        ChatRoomCreateOrGetResponse response = chatRoomService.createOrGetDirectRoom(9L, request);

        assertThat(response.isCreated()).isTrue();
        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getOpponentUserId()).isEqualTo(2L);
        verify(chatRoomRepository).findByDirectKey("2:9");
        ArgumentCaptor<ChatRoomMember> memberCaptor = ArgumentCaptor.forClass(ChatRoomMember.class);
        verify(memberRepository, times(2)).save(memberCaptor.capture());
        assertThat(memberCaptor.getAllValues())
                .extracting(member -> member.getUser().getUserId())
                .containsExactlyInAnyOrder(9L, 2L);
    }

    @Test
    @DisplayName("POST 기존 채팅방을 활성 멤버가 요청하면 방과 멤버를 중복 저장하지 않는다")
    void getExistingRoomForActiveMemberDoesNotCreateDuplicates() {
        User requester = user(1L);
        User opponent = user(2L);
        ChatRoom room = room(10L);
        ChatRoomMember requesterMember = member(1L, room, requester);
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(userQueryRepository.findByIdWithProfileImage(2L)).willReturn(Optional.of(opponent));
        given(chatRoomRepository.findByDirectKey("1:2")).willReturn(Optional.of(room));
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L))
                .willReturn(Optional.of(requesterMember));

        ChatRoomCreateOrGetResponse response =
                chatRoomService.createOrGetDirectRoom(1L, roomRequest(2L));

        assertThat(response.isCreated()).isFalse();
        assertThat(response.getOpponentUserId()).isEqualTo(2L);
        verify(chatRoomRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
        assertThat(requesterMember.getLeftAt()).isNull();
    }

    @Test
    @DisplayName("POST 기존 채팅방을 다시 열면 퇴장한 요청자와 상대방이 재입장한다")
    void getExistingRoomRejoinsMembers() {
        User requester = user(1L);
        User opponent = user(2L);
        ChatRoom room = room(10L);
        ChatRoomMember requesterMember = member(1L, room, requester);
        ChatRoomMember opponentMember = member(2L, room, opponent);
        requesterMember.leave();
        opponentMember.leave();
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(userQueryRepository.findByIdWithProfileImage(2L)).willReturn(Optional.of(opponent));
        given(chatRoomRepository.findByDirectKey("1:2")).willReturn(Optional.of(room));
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L))
                .willReturn(Optional.of(requesterMember));
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 2L))
                .willReturn(Optional.of(opponentMember));

        ChatRoomCreateOrGetResponse response =
                chatRoomService.createOrGetDirectRoom(1L, roomRequest(2L));

        assertThat(response.isCreated()).isFalse();
        assertThat(response.getOpponentUserId()).isEqualTo(2L);
        assertThat(requesterMember.getLeftAt()).isNull();
        assertThat(opponentMember.getLeftAt()).isNull();
        verify(memberRepository).findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L);
        verify(memberRepository).findByChatRoom_ChatRoomIdAndUser_userId(10L, 2L);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST 요청 사용자가 없으면 채팅방과 멤버를 저장하지 않는다")
    void createDirectRoomWithMissingRequesterDoesNotPersist() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.createOrGetDirectRoom(1L, roomRequest(2L)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(chatRoomRepository, memberRepository, userQueryRepository);
    }

    @Test
    @DisplayName("GET 채팅방 정보는 권한 검증 후 방과 상대방과 마지막 메시지를 반환한다")
    void getRoomInfoReturnsOpponentAndLastMessageAfterAuthorization() {
        ChatRoom room = room(10L);
        User opponent = user(2L);
        var lastMessage = message(100L, room, opponent);
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(memberRepository.findOpponentUserId(10L, 1L)).willReturn(Optional.of(2L));
        given(userQueryRepository.findByIdWithProfileImage(2L)).willReturn(Optional.of(opponent));
        given(messageRepository.findTopByChatRoom_ChatRoomIdOrderByChatMessageIdDesc(10L))
                .willReturn(Optional.of(lastMessage));

        ChatRoomInfoResponse response = chatRoomService.readInfo(1L, 10L);

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getOpponentUserId()).isEqualTo(2L);
        assertThat(response.getLastMessageId()).isEqualTo(100L);
        var inOrder = inOrder(authorizationService, chatRoomRepository);
        inOrder.verify(authorizationService).validateParticipant(10L, 1L);
        inOrder.verify(chatRoomRepository).findById(10L);
    }

    @Test
    @DisplayName("GET 마지막 메시지가 없는 채팅방 정보도 정상 반환한다")
    void getRoomInfoWithoutLastMessageSucceeds() {
        ChatRoom room = room(10L);
        User opponent = user(2L);
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(memberRepository.findOpponentUserId(10L, 1L)).willReturn(Optional.of(2L));
        given(userQueryRepository.findByIdWithProfileImage(2L)).willReturn(Optional.of(opponent));
        given(messageRepository.findTopByChatRoom_ChatRoomIdOrderByChatMessageIdDesc(10L))
                .willReturn(Optional.empty());

        ChatRoomInfoResponse response = chatRoomService.readInfo(1L, 10L);

        assertThat(response.getLastMessageId()).isNull();
        assertThat(response.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("GET 활성 요청자는 퇴장한 상대방의 채팅방 정보도 조회한다")
    void getRoomInfoReturnsLeftOpponent() {
        ChatRoom room = room(10L);
        User opponent = user(2L);
        var lastMessage = message(100L, room, opponent);
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
        given(memberRepository.findOpponentUserId(10L, 1L)).willReturn(Optional.of(2L));
        given(userQueryRepository.findByIdWithProfileImage(2L)).willReturn(Optional.of(opponent));
        given(messageRepository.findTopByChatRoom_ChatRoomIdOrderByChatMessageIdDesc(10L))
                .willReturn(Optional.of(lastMessage));

        ChatRoomInfoResponse response = chatRoomService.readInfo(1L, 10L);

        assertThat(response.getChatRoomId()).isEqualTo(10L);
        assertThat(response.getOpponentUserId()).isEqualTo(2L);
        verify(memberRepository).findOpponentUserId(10L, 1L);
    }

    @Test
    @DisplayName("GET 요청자가 퇴장한 채팅방은 권한 검증에서 조회를 중단한다")
    void getRoomInfoThrowsWhenRequesterLeft() {
        ForbiddenException exception = new ForbiddenException("해당 채팅방에 접근할 권한이 없습니다.");
        doThrow(exception).when(authorizationService).validateParticipant(10L, 1L);

        assertThatThrownBy(() -> chatRoomService.readInfo(1L, 10L))
                .isSameAs(exception);
        verifyNoInteractions(chatRoomRepository, memberRepository, userQueryRepository, messageRepository);
    }

    @Test
    @DisplayName("GET 채팅방 목록은 커서와 크기를 저장소에 그대로 전달한다")
    void getRoomListDelegatesCursorToRepository() {
        given(memberRepository.findChatRoomInfiniteScroll(1L, null, null, 20))
                .willReturn(List.of());

        assertThat(chatRoomService.readAllInfiniteScroll(1L, null, null, 20)).isEmpty();
        verify(memberRepository).findChatRoomInfiniteScroll(1L, null, null, 20);
    }

    @Test
    @DisplayName("DELETE 채팅방 퇴장은 권한 검증 후 요청자 멤버만 퇴장 처리한다")
    void deleteRoomLeavesRequesterAfterAuthorization() {
        ChatRoomMember member = member(1L, room(10L), user(1L));
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L))
                .willReturn(Optional.of(member));

        Long deletedRoomId = chatRoomService.delete(1L, 10L);

        assertThat(deletedRoomId).isEqualTo(10L);
        assertThat(member.getLeftAt()).isNotNull();
        var inOrder = inOrder(authorizationService, memberRepository);
        inOrder.verify(authorizationService).validateParticipant(10L, 1L);
        inOrder.verify(memberRepository).findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L);
    }

    @Test
    @DisplayName("DELETE 멤버를 찾지 못하면 퇴장 처리에 실패한다")
    void deleteRoomWithMissingMemberThrowsException() {
        given(memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(10L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.delete(1L, 10L))
                .isInstanceOf(NotFoundException.class);
    }
}
