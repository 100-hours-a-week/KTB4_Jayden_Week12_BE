package com.example.spring_rest_api.chat.service.response;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatResponse {
    private Long chatMessageId;
    private Long userId;
    private String content;
    private String chatType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static ChatResponse from(ChatMessage message) {
        ChatResponse response = new ChatResponse();
        response.chatMessageId = message.getChatMessageId();
        response.userId = message.getSender().getUserId();
        response.content = message.getContent();
        response.chatType = message.getChatType().toString();
        response.createdAt = message.getCreatedAt();
        response.updatedAt = message.getUpdatedAt();
        response.deletedAt = message.getDeletedAt();
        return response;
    }
}
