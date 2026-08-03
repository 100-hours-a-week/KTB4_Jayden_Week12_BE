package com.example.spring_rest_api.chat.repository;

import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import com.example.spring_rest_api.chat.service.response.ChatRoomListResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    List<Long> findUser_UserIdsByChatRoom_ChatRoomId(Long chatRoomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select member
        from ChatRoomMember member
        where member.chatRoom.chatRoomId = :roomId
            and member.user.userId = :userId
    """)
    Optional<ChatRoomMember> findByChatRoomIdAndUserIdForUpdate(Long roomId, Long userId);

    @Query("""
        select new com.example.spring_rest_api.chat.service.response.ChatRoomListResponse(
            room.chatRoomId,
            opponent.user.userId,
            opponent.user.nickname,
            profile.filePath,
            lastMessage.chatMessageId,
            lastMessage.content,
            lastMessage.createdAt,
            count(unreadMessage.chatMessageId)
        )
        from ChatRoomMember me
        join me.chatRoom room
        join ChatRoomMember opponent
            on opponent.chatRoom = room
            and opponent.user.userId <> :userId
            and opponent.leftAt is null

        left join opponent.user.profileImage profile

        left join ChatMessage lastMessage
            on lastMessage.chatRoom = room
            and not exists (
                select 1
                from ChatMessage newerMessage
                where newerMessage.chatRoom = room
                    and (
                        newerMessage.createdAt > lastMessage.createdAt
                        or (
                            newerMessage.createdAt = lastMessage.createdAt
                            and newerMessage.chatMessageId > lastMessage.chatMessageId
                        )
                    )
            )

        left join ChatMessage unreadMessage
            on unreadMessage.chatRoom = room
            and unreadMessage.sender.userId <> :userId
            and (
                me.lastReadMessage is null
                or unreadMessage.chatMessageId > me.lastReadMessage.chatMessageId
            )

        where me.user.userId = :userId
            and me.leftAt is null
            and (
                :createdAtCursor is null
                or lastMessage.createdAt < :createdAtCursor
            )

        group by
            room.chatRoomId,
            opponent.user.userId,
            opponent.user.nickname,
            profile.filePath,
            lastMessage.chatMessageId,
            lastMessage.content,
            lastMessage.createdAt

        order by
            lastMessage.createdAt desc,
            lastMessage.chatMessageId desc
        limit :pageSize
        """)
    List<ChatRoomListResponse> findChatRoomInfiniteScroll(Long userId, LocalDateTime createdAtCursor, int pageSize);
}
