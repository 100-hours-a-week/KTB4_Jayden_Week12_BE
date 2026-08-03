package com.example.spring_rest_api.chat.repository;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByChatMessageIdAndChatRoom_ChatRoomId(Long chatMessageId, Long chatRoomId);

    @Query("""
        select count(message)
        from ChatMessage message
        join ChatRoomMember member
            on member.chatRoom = message.chatRoom and member.user.userId = :userId
        where message.chatRoom.chatRoomId = :roomId
            and message.deletedAt is null
            and message.sender.userId <> :userId
            and (
                member.lastReadMessage is null
                    or message.chatMessageId > member.lastReadMessage.chatMessageId
            )
    """)
    Long countUnreadByRoomIdAndUserId(Long roomId, Long userId);
}
