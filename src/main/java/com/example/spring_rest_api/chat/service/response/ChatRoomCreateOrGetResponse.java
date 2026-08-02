package com.example.spring_rest_api.chat.service.response;

import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.image.util.ImageFileUtil;
import com.example.spring_rest_api.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomCreateOrGetResponse {
    private Long chatRoomId;
    private Long opponentUserId;
    private String nickname;
    private String profileImageUrl;
    private boolean created;

    private static ChatRoomCreateOrGetResponse from(ChatRoom chatRoom, User opponentUser, boolean created) {
        ChatRoomCreateOrGetResponse response = new ChatRoomCreateOrGetResponse();
        response.chatRoomId = chatRoom.getChatRoomId();
        response.opponentUserId = opponentUser.getUserId();
        response.nickname = opponentUser.getNickname();
        response.profileImageUrl = opponentUser.getProfileImage() == null ?
                null :
                ImageFileUtil.toFullUrl(opponentUser.getProfileImage().getFilePath());
        response.created = created;
        return  response;
    }

    public static ChatRoomCreateOrGetResponse create(ChatRoom chatRoom, User opponentUser) {
        return from(chatRoom, opponentUser, true);
    }

    public static ChatRoomCreateOrGetResponse find(ChatRoom chatRoom, User opponentUser) {
        return from(chatRoom, opponentUser, false);
    }
}
