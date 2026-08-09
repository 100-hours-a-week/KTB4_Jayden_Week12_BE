package com.example.spring_rest_api.chat.controller;

import com.example.spring_rest_api.chat.service.ChatService;
import com.example.spring_rest_api.chat.service.request.ChatReadRequest;
import com.example.spring_rest_api.chat.service.request.ChatRequest;
import com.example.spring_rest_api.chat.service.response.ChatReadResponse;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import com.example.spring_rest_api.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;

import static com.example.spring_rest_api.chat.ChatTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

    @InjectMocks
    ChatMessageController controller;

    @Mock
    ChatService chatService;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    private final Principal principal = () -> "1";

    @Test
    @DisplayName("메시지 전송 성공 후 채팅방 목록 업데이트를 한 번 발행한다")
    void sendTextPublishesRoomUpdateAfterSavingMessage() {
        ChatRequest request = chatRequest("client-1", "hello");
        ChatResponse response = ChatResponse.from(message(100L, room(10L), user(1L)));
        given(chatService.sendText(1L, 10L, request)).willReturn(response);

        controller.sendText(10L, request, principal);

        var inOrder = inOrder(chatService);
        inOrder.verify(chatService).sendText(1L, 10L, request);
        inOrder.verify(chatService).publishMessageUpdate(10L, response);
    }

    @Test
    @DisplayName("메시지 저장이 실패하면 채팅방 목록 업데이트를 발행하지 않는다")
    void sendTextFailureDoesNotPublishRoomUpdate() {
        ChatRequest request = chatRequest("client-1", "hello");
        given(chatService.sendText(1L, 10L, request))
                .willThrow(new ForbiddenException("denied"));

        assertThatThrownBy(() -> controller.sendText(10L, request, principal))
                .isInstanceOf(ForbiddenException.class);
        verify(chatService, never()).publishMessageUpdate(any(), any());
    }

    @Test
    @DisplayName("읽음 포인터가 변경되면 채팅방에 읽음 이벤트를 한 번 전송한다")
    void readMessageSendsEventWhenReadPointerChanges() {
        ChatReadRequest request = new ChatReadRequest();
        ReflectionTestUtils.setField(request, "lastReadMessageId", 100L);
        ChatReadResponse response = ChatReadResponse.updated(10L, 1L, 100L);
        given(chatService.markAsRead(1L, 10L, 100L)).willReturn(response);

        controller.readMessage(10L, request, principal);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chatrooms/10"), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOfSatisfying(ApiResponse.class, event -> {
            assertThat(event.getMessage()).isEqualTo("MESSAGE_READ");
            assertThat(event.getData()).isSameAs(response);
        });
    }

    @Test
    @DisplayName("읽음 포인터가 변경되지 않으면 채팅방 이벤트를 전송하지 않는다")
    void readMessageDoesNotSendEventWhenReadPointerDoesNotChange() {
        ChatReadRequest request = new ChatReadRequest();
        ReflectionTestUtils.setField(request, "lastReadMessageId", 100L);
        given(chatService.markAsRead(1L, 10L, 100L)).willReturn(null);

        controller.readMessage(10L, request, principal);

        verifyNoInteractions(messagingTemplate);
    }
}
