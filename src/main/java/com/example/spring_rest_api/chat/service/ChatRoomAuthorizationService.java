package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.repository.ChatRoomMemberRepository;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomAuthorizationService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    
    public void validateParticipant(Long chatRoomId, Long userId) {
        boolean isMember = chatRoomMemberRepository
                .findByChatRoom_ChatRoomIdAndUser_userId(chatRoomId, userId)
                .isPresent();
        if (!isMember) {
            throw new ForbiddenException("해당 채팅방에 접근할 권한이 없습니다.");
        }
    }
}
