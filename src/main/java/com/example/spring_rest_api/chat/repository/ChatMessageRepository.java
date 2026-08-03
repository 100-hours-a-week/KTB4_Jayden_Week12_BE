package com.example.spring_rest_api.chat.repository;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByChatMessageIdAndChatRoom_ChatRoomId(Long chatMessageId, Long chatRoomId);

    @Query("""
        select count(message)
        from ChatMessage message
        join ChatRoomMember member
            on member.chatRoom = message.chatRoom and member.user.userId = :userId
        where
            message.deletedAt is null
            and message.sender.userId <> :userId
            and (
                member.lastReadMessage is null
                    or message.chatMessageId > member.lastReadMessage.chatMessageId
            )
    """)
    Long countUnreadAll(Long userId);

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

    Optional<ChatMessage> findTopByChatRoom_ChatRoomIdOrderByChatMessageIdDesc(Long chatRoomId);

    @Query("""
        select message
        from ChatMessage message
        where message.chatRoom.chatRoomId = :roomId
            and (
                :lastMessageId is null
                    or message.chatMessageId < :lastMessageId
            )
        order by
            message.chatMessageId desc
        limit :pageSize
    """)
    List<ChatMessage> findAllInfiniteScroll(Long roomId, Long lastMessageId, int pageSize);
}
