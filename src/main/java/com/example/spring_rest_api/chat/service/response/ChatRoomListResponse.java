package com.example.spring_rest_api.chat.service.response;

import com.example.spring_rest_api.image.util.ImageFileUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@NoArgsConstructor
public class ChatRoomListResponse {
    private Long chatRoomId;

    private Long opponentUserId;
    private String nickname;
    private String profileImageUrl;

    private Long lastMessageId;
    private String content;
    private LocalDateTime createdAt;

    private Long unreadCount;

    public ChatRoomListResponse(Long chatRoomId, Long opponentUserId, String nickname, String profileImagePath, Long lastMessageId, String content, LocalDateTime createdAt, Long unreadCount) {
        this.chatRoomId = chatRoomId;
        this.opponentUserId = opponentUserId;
        this.nickname = nickname;
        this.profileImageUrl = Optional.ofNullable(profileImagePath)
                .map(ImageFileUtil::toFullUrl)
                .orElse(null);
        this.lastMessageId = lastMessageId;
        this.content = content;
        this.createdAt = createdAt;
        this.unreadCount = unreadCount;
    }
}
