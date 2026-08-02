package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.entity.ChatRoomMember;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;

    public static ChatRoomListResponse readInfiniteScroll(Long userId, LocalDateTime createdAtCursor, Long pageSize) {
        return null;
    }

    @Transactional
    public ChatRoomCreateOrGetResponse createOrGetDirectRoom(Long userId, ChatRoomCreateOrGetRequest request) {
        User requestUser = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        User opponentUser = userQueryRepository.findByIdWithProfileImage(request.getOpponentId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("OPPONENT_USER_NOT_FOUND"));

        String directKey = generateDirectKey(userId, opponentUser.getUserId());

        return chatRoomRepository.findByDirectKey(directKey)
                .map(r -> ChatRoomCreateOrGetResponse.find(r, opponentUser))
                .orElseGet(() -> createRoom(requestUser, opponentUser, directKey));
    }

    public static ChatRoomInfoResponse readInfo(Long userId, Long roomId) {
        return null;
    }




    private String generateDirectKey(Long userId, Long opponentUserId) {
        return userId < opponentUserId ?
                String.format("%s:%s", userId, opponentUserId) :
                String.format("%s:%s", opponentUserId, userId);
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
