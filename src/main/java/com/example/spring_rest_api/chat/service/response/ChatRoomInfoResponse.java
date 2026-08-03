package com.example.spring_rest_api.chat.service.response;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.image.util.ImageFileUtil;
import com.example.spring_rest_api.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatRoomInfoResponse {
    private Long chatRoomId;
    private Long opponentUserId;
    private String nickname;
    private String profileImageUrl;
    private Long lastMessageId;
    private LocalDateTime createdAt;

    public static ChatRoomInfoResponse from(ChatRoom chatRoom, User opponentUser, ChatMessage message) {
        ChatRoomInfoResponse response = new ChatRoomInfoResponse();
        response.chatRoomId = chatRoom.getChatRoomId();
        response.opponentUserId = opponentUser.getUserId();
        response.nickname = opponentUser.getNickname();
        response.profileImageUrl = opponentUser.getProfileImage() == null ?
                null :
                ImageFileUtil.toFullUrl(opponentUser.getProfileImage().getFilePath());

        response.lastMessageId = message == null ? null : message.getChatMessageId();
        response.createdAt = message == null ? null : message.getCreatedAt();
        return  response;
    }
}
