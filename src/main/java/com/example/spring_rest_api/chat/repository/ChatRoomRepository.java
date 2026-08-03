package com.example.spring_rest_api.chat.repository;

import com.example.spring_rest_api.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByDirectKey(String directKey);

    @Query("""
        select room
        from ChatRoomMember member
        join fetch member.chatRoom room
        join fetch member.lastReadMessage message
        where member.user.userId = :userId
        order by message.createdAt desc, room.chatRoomId desc
        limit :pageSize
    """)
    List<ChatRoom> findChatRoomInfiniteScroll(Long userId, Long pageSize);

    @Query("""
        select room
        from ChatRoomMember member
        join fetch member.chatRoom room
        join fetch member.lastReadMessage message
        where member.user.userId = :userId
            and message.createdAt > :createdAtCursor
            and room.chatRoomId > :roomId
        order by message.createdAt desc, room.chatRoomId desc
        limit :pageSize
    """)
    List<ChatRoom> findChatRoomInfiniteScroll(Long userId, LocalDateTime createdAtCursor, Long roomId, Long pageSize);
}
