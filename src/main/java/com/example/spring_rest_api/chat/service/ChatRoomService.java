package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.repository.ChatRoomRepository;
import com.example.spring_rest_api.chat.service.request.ChatRoomCreateRequest;
import com.example.spring_rest_api.chat.service.response.ChatRoomResponse;
import com.example.spring_rest_api.common.exception.NotFoundException;
import com.example.spring_rest_api.user.entity.User;
import com.example.spring_rest_api.user.repository.UserQueryRepository;
import com.example.spring_rest_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;

    @Transactional
    public ChatRoomResponse createOrGetDirectRoom(Long userId, ChatRoomCreateRequest request) {
        userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        User opponentUser = userQueryRepository.findByIdWithProfileImage(request.getOpponentId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("OPPONENT_USER_NOT_FOUND"));

        String directKey = generateDirectKey(userId, opponentUser.getUserId());

        ChatRoom room = chatRoomRepository.findByDirectKey(directKey)
                .orElseGet(() -> createRoom(userId, opponentUser.getUserId(), directKey));

        return ChatRoomResponse.from(
                room,
                opponentUser
        );
    }

    private String generateDirectKey(Long userId, Long opponentUserId) {
        return userId < opponentUserId ?
                String.format("%s:%s", userId, opponentUserId) :
                String.format("%s:%s", opponentUserId, userId);
    }

    private ChatRoom createRoom(Long userId, Long opponentUserId, String directKey) {
        return chatRoomRepository.save(ChatRoom.createDirect(userId, opponentUserId, directKey));
    }
}
