package com.example.spring_rest_api.chat;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import com.example.spring_rest_api.chat.service.request.ChatRequest;
import com.example.spring_rest_api.chat.service.request.ChatRoomCreateOrGetRequest;
import com.example.spring_rest_api.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

public final class ChatTestFixtures {

    private ChatTestFixtures() {
    }

    public static User user(Long userId) {
        User user = User.create(
                "user" + userId + "@example.com",
                "password",
                "user" + userId,
                null
        );
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    public static ChatRoom room(Long roomId) {
        ChatRoom room = ChatRoom.createDirect(1L, 2L, "1:2");
        ReflectionTestUtils.setField(room, "chatRoomId", roomId);
        return room;
    }

    public static ChatMessage message(Long messageId, ChatRoom room, User sender) {
        ChatMessage message = ChatMessage.createTextMessage(
                room,
                sender,
                "client-" + messageId,
                "message-" + messageId
        );
        ReflectionTestUtils.setField(message, "chatMessageId", messageId);
        return message;
    }

    public static ChatRoomMember member(Long memberId, ChatRoom room, User user) {
        ChatRoomMember member = ChatRoomMember.addMember(room, user);
        ReflectionTestUtils.setField(member, "chatRoomMemberId", memberId);
        return member;
    }

    public static ChatRequest chatRequest(String clientMessageId, String content) {
        ChatRequest request = new ChatRequest();
        ReflectionTestUtils.setField(request, "clientMessageId", clientMessageId);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    public static ChatRoomCreateOrGetRequest roomRequest(Long opponentId) {
        ChatRoomCreateOrGetRequest request = new ChatRoomCreateOrGetRequest();
        ReflectionTestUtils.setField(request, "opponentId", opponentId);
        return request;
    }
}
