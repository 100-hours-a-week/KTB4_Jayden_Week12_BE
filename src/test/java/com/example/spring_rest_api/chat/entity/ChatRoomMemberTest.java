package com.example.spring_rest_api.chat.entity;

import com.example.spring_rest_api.common.exception.BadRequestException;
import com.example.spring_rest_api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.example.spring_rest_api.chat.ChatTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomMemberTest {

    @Test
    @DisplayName("채팅방 멤버를 추가하면 활성 상태로 생성된다")
    void addMemberCreatesActiveMember() {
        ChatRoomMember member = member(1L, room(10L), user(1L));

        assertThat(member.getJoinedAt()).isNotNull();
        assertThat(member.getLeftAt()).isNull();
        assertThat(member.getLastReadMessage()).isNull();
    }

    @Test
    @DisplayName("더 최신인 같은 방 메시지로만 읽음 포인터가 전진한다")
    void readThroughAdvancesOnlyToNewerMessage() {
        ChatRoom room = room(10L);
        User sender = user(2L);
        ChatRoomMember member = member(1L, room, user(1L));
        ChatMessage older = message(100L, room, sender);
        ChatMessage newer = message(101L, room, sender);

        assertThat(member.readThrough(newer)).isTrue();
        assertThat(member.readThrough(newer)).isFalse();
        assertThat(member.readThrough(older)).isFalse();
        assertThat(member.getLastReadMessage()).isSameAs(newer);
    }

    @Test
    @DisplayName("다른 채팅방 메시지로 읽음 처리하면 포인터를 변경하지 않는다")
    void readThroughMessageFromAnotherRoomThrowsException() {
        ChatRoomMember member = member(1L, room(10L), user(1L));
        ChatMessage anotherRoomMessage = message(100L, room(20L), user(2L));

        assertThatThrownBy(() -> member.readThrough(anotherRoomMessage))
                .isInstanceOf(BadRequestException.class);
        assertThat(member.getLastReadMessage()).isNull();
    }

    @Test
    @DisplayName("퇴장은 반복 호출해도 최초 퇴장 시각을 유지한다")
    void leaveIsIdempotent() {
        ChatRoomMember member = member(1L, room(10L), user(1L));

        member.leave();
        LocalDateTime firstLeftAt = member.getLeftAt();
        member.leave();

        assertThat(member.getLeftAt()).isEqualTo(firstLeftAt);
    }

    @Test
    @DisplayName("퇴장 멤버가 재입장하면 퇴장 상태와 읽음 포인터가 초기화된다")
    void rejoinResetsLeaveAndReadState() {
        ChatRoom room = room(10L);
        ChatRoomMember member = member(1L, room, user(1L));
        member.readThrough(message(100L, room, user(2L)));
        member.leave();
        LocalDateTime previousJoinedAt = member.getJoinedAt();

        member.rejoin();

        assertThat(member.getLeftAt()).isNull();
        assertThat(member.getLastReadMessage()).isNull();
        assertThat(member.getJoinedAt()).isAfterOrEqualTo(previousJoinedAt);
    }

    @Test
    @DisplayName("활성 멤버의 재입장 호출은 상태를 변경하지 않는다")
    void rejoinActiveMemberDoesNotChangeState() {
        ChatRoomMember member = member(1L, room(10L), user(1L));
        LocalDateTime joinedAt = member.getJoinedAt();

        member.rejoin();

        assertThat(member.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(member.getLeftAt()).isNull();
    }
}
