package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.service.response.ChatMessagesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository messageRepository;
    private final ChatRoomAuthorizationService roomAuthorizationService;

    public List<ChatMessagesResponse> readMessagesInfiniteScroll(Long userId, Long roomId, Long lastMessageId, int pageSize) {
        roomAuthorizationService.validateParticipant(roomId, userId);
        return messageRepository.findAllInfiniteScroll(roomId, lastMessageId, pageSize).stream()
                .map(ChatMessagesResponse::from)
                .toList().reversed();
    }

    public Long countUnreadMessages(Long userId) {
        return messageRepository.countUnreadAll(userId);
    }
}
