package com.example.spring_rest_api.chat.service.response;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import com.example.spring_rest_api.chat.entity.ChatType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatMessagesResponse {
    private Long messageId;
    private Long senderId;
    private String content;
    private ChatType chatType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public ChatMessagesResponse from(ChatMessage chatMessage) {
        ChatMessagesResponse response = new ChatMessagesResponse();
        response.messageId = chatMessage.getChatMessageId();
        response.senderId = chatMessage.getSender().getUserId();
        response.content = chatMessage.getContent();
        response.chatType = chatMessage.getChatType();
        response.createdAt = chatMessage.getCreatedAt();
        response.updatedAt = chatMessage.getUpdatedAt();
        response.deletedAt = chatMessage.getDeletedAt();
        return response;
    }
}
