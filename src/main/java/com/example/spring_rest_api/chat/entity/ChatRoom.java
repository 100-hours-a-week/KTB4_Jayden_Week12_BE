package com.example.spring_rest_api.chat.entity;

import com.example.spring_rest_api.common.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomId;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private RoomType roomType;

    private String directKey = "00:00"; //TODO - 유니크 제약조건

    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY)
    private List<ChatMessage> chatMessages = new ArrayList<>();


    public static ChatRoom createDirect(Long userIdOne, Long userIdAnother, String directKey) {
        if (userIdOne.equals(userIdAnother)) {
            throw new BadRequestException("자기 자신과 채팅방을 만들 수 없습니다.");
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.roomType = RoomType.DIRECT;
        chatRoom.directKey = directKey;
        chatRoom.createdAt = LocalDateTime.now();
        return chatRoom;
    }
}