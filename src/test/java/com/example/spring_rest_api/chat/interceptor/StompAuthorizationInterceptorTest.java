package com.example.spring_rest_api.chat.interceptor;

import com.example.spring_rest_api.chat.principal.StompPrincipal;
import com.example.spring_rest_api.chat.service.ChatRoomAuthorizationService;
import com.example.spring_rest_api.common.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StompAuthorizationInterceptorTest {

    @InjectMocks
    StompAuthorizationInterceptor interceptor;

    @Mock
    ChatRoomAuthorizationService authorizationService;

    @Mock
    MessageChannel channel;

    @Test
    @DisplayName("채팅방 SUBSCRIBE는 Principal 사용자와 destination 방의 참여 권한을 검증한다")
    void subscribeChatRoomValidatesParticipant() {
        Message<byte[]> message = message(
                StompCommand.SUBSCRIBE,
                "/sub/chatrooms/10",
                new StompPrincipal(1L, Instant.now().plusSeconds(60))
        );

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verify(authorizationService).validateParticipant(10L, 1L);
    }

    @Test
    @DisplayName("Principal이 없는 SUBSCRIBE는 인증 필요로 거부한다")
    void subscribeWithoutPrincipalThrowsException() {
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, "/sub/chatrooms/10", null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("인증이 필요합니다.");
        verifyNoInteractions(authorizationService);
    }

    @Test
    @DisplayName("채팅방 외 destination 구독은 방 참여 권한 검증을 생략한다")
    void subscribeNonChatDestinationSkipsRoomAuthorization() {
        Message<byte[]> message = message(
                StompCommand.SUBSCRIBE,
                "/user/queue/chat-updates",
                new StompPrincipal(1L, Instant.now().plusSeconds(60))
        );

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(authorizationService);
    }

    @Test
    @DisplayName("SUBSCRIBE가 아닌 STOMP command는 방 참여 권한 검증을 생략한다")
    void nonSubscribeCommandSkipsRoomAuthorization() {
        Message<byte[]> message = message(
                StompCommand.SEND,
                "/pub/chatrooms/10/messages",
                new StompPrincipal(1L, Instant.now().plusSeconds(60))
        );

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(authorizationService);
    }

    @Test
    @DisplayName("숫자가 아닌 채팅방 ID로 SUBSCRIBE하면 메시징 예외로 거부한다")
    void subscribeMalformedRoomIdThrowsMessageDeliveryException() {
        Message<byte[]> message = message(
                StompCommand.SUBSCRIBE,
                "/sub/chatrooms/not-a-number",
                new StompPrincipal(1L, Instant.now().plusSeconds(60))
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("유효하지 않은 채팅방 구독 경로입니다.");
        verifyNoInteractions(authorizationService);
    }

    private Message<byte[]> message(
            StompCommand command,
            String destination,
            StompPrincipal principal
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

}
