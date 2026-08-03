package com.example.spring_rest_api.chat.repository;

import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoom_ChatRoomIdAndUser_userId(Long roomId, Long senderId);

    @Query("""
        select member.user.userId
        from ChatRoomMember member
        where member.chatRoom.chatRoomId = :roomId
    """)
    List<Long> findUserIdsByRoomId(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select member
        from ChatRoomMember member
        where member.chatRoom.chatRoomId = :roomId
            and member.user.userId = :userId
    """)
    Optional<ChatRoomMember> findByRoomIdAndUserIdForUpdate(Long roomId, Long userId);
}
