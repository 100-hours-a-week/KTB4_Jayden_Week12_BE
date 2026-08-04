package com.example.spring_rest_api.chat.service.response;

import com.example.spring_rest_api.chat.entity.ChatType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomUpdateResponse {
    private ChatUpdateType chatUpdateType;
    private String clientMessageId;
    private Long roomId;
    private Long messageId;
    private Long senderId;
    private String content;
    private ChatType chatType;
    private LocalDateTime createdAt;
    private Long unreadCount;
}
