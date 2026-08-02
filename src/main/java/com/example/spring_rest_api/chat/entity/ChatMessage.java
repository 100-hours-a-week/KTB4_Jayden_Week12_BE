package com.example.spring_rest_api.chat.entity;

import com.example.spring_rest_api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    private String content;

    @Enumerated(EnumType.STRING)
    private ChatType chatType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static ChatMessage createTextMessage(ChatRoom chatRoom, User sender, String content) {
        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = sender;
        message.content = content;
        message.chatType = ChatType.TEXT;
        message.createdAt = LocalDateTime.now();
        message.updatedAt = null;
        message.deletedAt = null;
        return message;
    }
}
