package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.service.response.ChatMessagesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;

    public List<ChatMessagesResponse> readMessages(Long userId, Long roomId, Long lastMessageId, Long pageSize) {
        return null;
    }

    public Long countUnreadMessages(Long userId) {
        return null;
    }
}
