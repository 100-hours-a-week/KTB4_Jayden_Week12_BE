package com.example.spring_rest_api.chat.repository;

import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    Optional<ChatRoomMember> findByChatRoom_ChatRoomIdAndUser_userId(Long roomId, Long senderId);
}
