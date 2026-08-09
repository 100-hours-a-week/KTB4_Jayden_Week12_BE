package com.example.spring_rest_api.chat.entity;

import com.example.spring_rest_api.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomTest {

    @Test
    @DisplayName("서로 다른 사용자로 1대1 채팅방을 생성하면 DIRECT 유형과 키가 설정된다")
    void createDirectRoomSetsRoomProperties() {
        ChatRoom room = ChatRoom.createDirect(1L, 2L, "1:2");

        assertThat(room.getRoomType()).isEqualTo(RoomType.DIRECT);
        assertThat(room.getDirectKey()).isEqualTo("1:2");
        assertThat(room.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("자기 자신과 1대1 채팅방을 생성하면 예외가 발생한다")
    void createDirectRoomWithSelfThrowsException() {
        assertThatThrownBy(() -> ChatRoom.createDirect(1L, 1L, "1:1"))
                .isInstanceOf(BadRequestException.class);
    }
}
