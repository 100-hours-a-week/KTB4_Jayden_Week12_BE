package com.example.spring_rest_api.chat.entity;

import com.example.spring_rest_api.common.exception.BadRequestException;
import com.example.spring_rest_api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatRoomMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ChatMessage lastReadMessage;

    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    public static ChatRoomMember addMember(ChatRoom chatRoom, User user) {
        ChatRoomMember member = new ChatRoomMember();
        member.chatRoom =  chatRoom;
        member.user = user;
        member.joinedAt = LocalDateTime.now();
        member.leftAt = null;
        return member;
    }

    public boolean readThrough(ChatMessage lastReadMessage) {
        validateMessageRoom(lastReadMessage);
        if (this.lastReadMessage != null && this.lastReadMessage.getChatMessageId() >= lastReadMessage.getChatMessageId()) {
            return false;
        }

        this.lastReadMessage = lastReadMessage;
        return true;
    }

    public void leave() {
        if (this.leftAt != null) {
            return;
        }
        this.leftAt = LocalDateTime.now();
    }

    public void rejoin() {
        if (this.leftAt == null) {
            return;
        }
        this.joinedAt = LocalDateTime.now();
        this.leftAt = null;
        this.lastReadMessage = null;
    }

    private void validateMessageRoom(ChatMessage message) {
        Long memberRoomId = chatRoom.getChatRoomId();
        Long messageRoomId = message.getChatRoom().getChatRoomId();
        if (!memberRoomId.equals(messageRoomId)) {
            throw new BadRequestException("INVALID_UPDATE_REQUEST");
        }
    }
}
