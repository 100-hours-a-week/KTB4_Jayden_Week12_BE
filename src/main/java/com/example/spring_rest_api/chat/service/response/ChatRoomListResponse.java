package com.example.spring_rest_api.chat.service.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatRoomListResponse {
    private Long chatRoomId;

    private Long opponentUserId;
    private String nickname;
    private String profileImageUrl;

    private Long lastReadMessageId;
    private String content;
    private LocalDateTime createdAt;

    private Long unreadCount;

    public ChatRoomListResponse(Long chatRoomId, Long opponentUserId, String nickname, String profileImageUrl, Long lastReadMessageId, String content, LocalDateTime createdAt, Long unreadCount) {
        this.chatRoomId = chatRoomId;
        this.opponentUserId = opponentUserId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.lastReadMessageId = lastReadMessageId;
        this.content = content;
        this.createdAt = createdAt;
        this.unreadCount = unreadCount;
    }
}
