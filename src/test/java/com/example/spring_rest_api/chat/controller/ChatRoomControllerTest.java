package com.example.spring_rest_api.chat.controller;

import com.example.spring_rest_api.chat.service.ChatMessageService;
import com.example.spring_rest_api.chat.service.ChatRoomService;
import com.example.spring_rest_api.chat.service.response.ChatMessagesResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomInfoResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomListResponse;
import com.example.spring_rest_api.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerTest {

    @InjectMocks
    ChatRoomController controller;

    @Mock
    ChatRoomService chatRoomService;

    @Mock
    ChatMessageService chatMessageService;

    @Test
    @DisplayName("GET 채팅방 정보는 인증 사용자와 방 ID로 서비스를 호출한다")
    void getRoomInfoDelegatesAuthenticatedUserAndRoomId() {
        ChatRoomInfoResponse serviceResponse = mock(ChatRoomInfoResponse.class);
        given(chatRoomService.readInfo(1L, 10L)).willReturn(serviceResponse);

        ResponseEntity<ApiResponse<ChatRoomInfoResponse>> response =
                controller.readChatRoomInfo(1L, 10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("chat_room_info_read_success");
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(chatRoomService).readInfo(1L, 10L);
    }

    @Test
    @DisplayName("GET 채팅방 목록은 인증 사용자와 커서와 페이지 크기를 서비스에 전달한다")
    void getRoomListDelegatesCursorAndPageSize() {
        LocalDateTime cursor = LocalDateTime.of(2026, 8, 7, 10, 0);
        List<ChatRoomListResponse> serviceResponse = List.of();
        given(chatRoomService.readAllInfiniteScroll(1L, cursor, 100L, 20))
                .willReturn(serviceResponse);

        ResponseEntity<ApiResponse<List<ChatRoomListResponse>>> response =
                controller.readChatRoomInfiniteScroll(1L, cursor, 100L, 20);

        assertThat(response.getBody().getMessage()).isEqualTo("chat_room_list_read_success");
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(chatRoomService).readAllInfiniteScroll(1L, cursor, 100L, 20);
    }

    @Test
    @DisplayName("GET 채팅 메시지 목록은 인증 사용자와 방과 커서를 서비스에 전달한다")
    void getRoomMessagesDelegatesRoomAndCursor() {
        List<ChatMessagesResponse> serviceResponse = List.of();
        given(chatMessageService.readMessagesInfiniteScroll(1L, 10L, 100L, 20))
                .willReturn(serviceResponse);

        ResponseEntity<ApiResponse<List<ChatMessagesResponse>>> response =
                controller.readChatRoomMessages(1L, 10L, 100L, 20);

        assertThat(response.getBody().getMessage()).isEqualTo("messages_read_success");
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(chatMessageService).readMessagesInfiniteScroll(1L, 10L, 100L, 20);
    }

    @Test
    @DisplayName("GET 전체 읽지 않은 메시지 수는 인증 사용자로 서비스를 호출한다")
    void getUnreadCountDelegatesAuthenticatedUser() {
        given(chatMessageService.countUnreadMessages(1L)).willReturn(7L);

        ResponseEntity<ApiResponse<Long>> response = controller.readUnreadCount(1L);

        assertThat(response.getBody().getMessage()).isEqualTo("unread_count_load_success");
        assertThat(response.getBody().getData()).isEqualTo(7L);
        verify(chatMessageService).countUnreadMessages(1L);
    }

    @Test
    @DisplayName("DELETE 채팅방 퇴장은 인증 사용자와 방 ID로 서비스를 호출한다")
    void deleteRoomDelegatesAuthenticatedUserAndRoomId() {
        given(chatRoomService.delete(1L, 10L)).willReturn(10L);

        ResponseEntity<ApiResponse<Long>> response = controller.deleteChatRoom(1L, 10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("chat_room_delete_success");
        assertThat(response.getBody().getData()).isEqualTo(10L);
        verify(chatRoomService).delete(1L, 10L);
    }
}
