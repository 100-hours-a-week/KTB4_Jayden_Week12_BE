package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomMemberRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomRepository;
import com.example.spring_rest_api.chat.service.request.ChatRoomCreateOrGetRequest;
import com.example.spring_rest_api.chat.service.response.ChatRoomCreateOrGetResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomInfoResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomListResponse;
import com.example.spring_rest_api.common.exception.NotFoundException;
import com.example.spring_rest_api.user.entity.User;
import com.example.spring_rest_api.user.repository.UserQueryRepository;
import com.example.spring_rest_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatRoomAuthorizationService roomAuthorizationService;
    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;

    @Transactional
    public ChatRoomCreateOrGetResponse createOrGetDirectRoom(Long userId, ChatRoomCreateOrGetRequest request) {
        User requestUser = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        User opponentUser = userQueryRepository.findByIdWithProfileImage(request.getOpponentId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("OPPONENT_USER_NOT_FOUND"));

        String directKey = generateDirectKey(userId, opponentUser.getUserId());
        Optional<ChatRoom> roomOptional = chatRoomRepository.findByDirectKey(directKey);

        return roomOptional
                .map(r -> findRoom(r, opponentUser))
                .orElseGet(() -> createRoom(requestUser, opponentUser, directKey));
    }

    public ChatRoomInfoResponse readInfo(Long userId, Long roomId) {
        roomAuthorizationService.validateParticipant(roomId, userId);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND"));

        List<Long> userIds = memberRepository.findUser_UserIdsByChatRoom_ChatRoomId(roomId);
        Long opponentUserId = userIds.stream()
                .filter(l -> !l.equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("OPPONENT_USERID_NOT_FOUND"));

        User opponentUser = userQueryRepository.findByIdWithProfileImage(opponentUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("OPPONENT_USER_NOT_FOUND"));

        ChatMessage lastMessage = messageRepository.findTopByChatRoom_ChatRoomIdOrderByChatMessageIdDesc(roomId)
                .orElse(null);

        return ChatRoomInfoResponse.from(
                room,
                opponentUser,
                lastMessage
        );
    }

    public List<ChatRoomListResponse> readAllInfiniteScroll(Long userId, LocalDateTime createdAtCursor, Long lastMessageId, int pageSize) {
        return memberRepository.findChatRoomInfiniteScroll(
                userId,
                createdAtCursor,
                lastMessageId,
                pageSize
        );
    }

    @Transactional
    public Long delete(Long userId, Long roomId) {
        roomAuthorizationService.validateParticipant(roomId, userId);
        ChatRoomMember member = memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(roomId, userId)
                .orElseThrow(() -> new NotFoundException("MEMBER_NOT_FOUND"));
        member.leave();

        return roomId;
    }




    private String generateDirectKey(Long userId, Long opponentUserId) {
        return userId < opponentUserId ?
                String.format("%s:%s", userId, opponentUserId) :
                String.format("%s:%s", opponentUserId, userId);
    }

    private ChatRoomCreateOrGetResponse findRoom(ChatRoom room, User opponentUser) {
        memberRepository.findByChatRoom_ChatRoomIdAndUser_userId(
                        room.getChatRoomId(),
                        opponentUser.getUserId()
                )
                .filter(m -> m.getLeftAt() != null)
                .ifPresent(ChatRoomMember::rejoin);

        return ChatRoomCreateOrGetResponse.find(room, opponentUser);
    }

    private ChatRoomCreateOrGetResponse createRoom(User requestUser, User opponentUser, String directKey) {
        ChatRoom room = chatRoomRepository.save(ChatRoom.createDirect(
                requestUser.getUserId(),
                opponentUser.getUserId(),
                directKey
        ));

        memberRepository.save(ChatRoomMember.addMember(room, requestUser));
        memberRepository.save(ChatRoomMember.addMember(room, opponentUser));

        return ChatRoomCreateOrGetResponse.create(room,  opponentUser);

    }
}
