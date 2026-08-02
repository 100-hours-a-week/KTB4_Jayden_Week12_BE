package com.example.spring_rest_api.chat.service.response;

import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.image.util.ImageFileUtil;
import com.example.spring_rest_api.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomResponse {
    private Long chatRoomId;
    private Long opponentUserId;
    private String nickname;
    private String profileImageUrl;

    public static ChatRoomResponse from(ChatRoom chatRoom, User opponentUser) {
        ChatRoomResponse response = new ChatRoomResponse();
        response.chatRoomId = chatRoom.getChatRoomId();
        response.opponentUserId = opponentUser.getUserId();
        response.nickname = opponentUser.getNickname();
        response.profileImageUrl = opponentUser.getProfileImage() == null ? null : ImageFileUtil.toFullUrl(opponentUser.getProfileImage().getFilePath());
        return response;
    }
}
