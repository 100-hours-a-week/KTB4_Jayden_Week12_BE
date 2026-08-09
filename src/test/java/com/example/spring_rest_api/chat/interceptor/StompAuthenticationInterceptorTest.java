package com.example.spring_rest_api.chat.interceptor;

import com.example.spring_rest_api.authorization.jwt.JwtProvider;
import com.example.spring_rest_api.chat.principal.StompPrincipal;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompAuthenticationInterceptorTest {

    @InjectMocks
    StompAuthenticationInterceptor interceptor;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    MessageChannel channel;

    @Test
    @DisplayName("유효한 Bearer 토큰으로 CONNECT하면 STOMP Principal을 설정한다")
    void connectWithValidTokenSetsPrincipal() {
        StompPrincipal principal = new StompPrincipal(1L, Instant.now().plusSeconds(60));
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null, null);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        Message<byte[]> message = message(accessor);
        given(jwtProvider.verifyAccessToken("valid-token")).willReturn(principal);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(StompHeaderAccessor.wrap(result).getUser()).isEqualTo(principal);
        verify(jwtProvider).verifyAccessToken("valid-token");
    }

    @Test
    @DisplayName("Authorization 헤더 없이 CONNECT하면 인증을 거부한다")
    void connectWithoutAuthorizationHeaderThrowsException() {
        Message<byte[]> message = message(accessor(StompCommand.CONNECT, null, null));

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("WebSocket 인증 토큰이 없습니다.");
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 토큰으로 CONNECT하면 인증을 거부한다")
    void connectWithMalformedAuthorizationHeaderThrowsException() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null, null);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Basic token");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("WebSocket 인증 토큰이 없습니다.");
    }

    @Test
    @DisplayName("JWT 검증에 실패한 CONNECT는 유효하지 않은 토큰으로 거부한다")
    void connectWithInvalidTokenThrowsException() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, null, null);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        given(jwtProvider.verifyAccessToken("invalid-token"))
                .willThrow(new JwtException("invalid"));

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("유효하지 않은 WebSocket 토큰입니다.");
    }

    @Test
    @DisplayName("인증된 만료 전 세션의 SEND와 SUBSCRIBE를 허용한다")
    void authenticatedSessionAllowsSendAndSubscribe() {
        StompPrincipal principal = new StompPrincipal(1L, Instant.now().plusSeconds(60));
        Message<byte[]> send = message(accessor(StompCommand.SEND, "/pub/chatrooms/10/messages", principal));
        Message<byte[]> subscribe = message(accessor(StompCommand.SUBSCRIBE, "/sub/chatrooms/10", principal));

        assertThat(interceptor.preSend(send, channel)).isSameAs(send);
        assertThat(interceptor.preSend(subscribe, channel)).isSameAs(subscribe);
    }

    @Test
    @DisplayName("Principal이 없는 SEND는 WebSocket 인증 필요로 거부한다")
    void sendWithoutPrincipalThrowsException() {
        Message<byte[]> send = message(accessor(StompCommand.SEND, "/pub/chatrooms/10/messages", null));

        assertThatThrownBy(() -> interceptor.preSend(send, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("WebSocket 인증이 필요합니다.");
    }

    @Test
    @DisplayName("만료된 Principal의 SUBSCRIBE를 거부한다")
    void subscribeWithExpiredPrincipalThrowsException() {
        StompPrincipal expired = new StompPrincipal(1L, Instant.now().minusSeconds(1));
        Message<byte[]> subscribe =
                message(accessor(StompCommand.SUBSCRIBE, "/sub/chatrooms/10", expired));

        assertThatThrownBy(() -> interceptor.preSend(subscribe, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("WebSocket 액세스 토큰이 만료되었습니다.");
    }

    @Test
    @DisplayName("같은 사용자의 새 토큰으로 재인증하면 Principal을 교체한다")
    void reauthenticateSameUserReplacesPrincipal() {
        StompPrincipal current = new StompPrincipal(1L, Instant.now().plusSeconds(10));
        StompPrincipal renewed = new StompPrincipal(1L, Instant.now().plusSeconds(120));
        StompHeaderAccessor accessor = accessor(StompCommand.SEND, "/pub/auth/reauth", current);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer renewed-token");
        Message<byte[]> message = message(accessor);
        given(jwtProvider.verifyAccessToken("renewed-token")).willReturn(renewed);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(StompHeaderAccessor.wrap(result).getUser()).isEqualTo(renewed);
    }

    @Test
    @DisplayName("다른 사용자의 토큰으로 재인증하면 기존 Principal을 유지하고 거부한다")
    void reauthenticateDifferentUserThrowsException() {
        StompPrincipal current = new StompPrincipal(1L, Instant.now().plusSeconds(10));
        StompPrincipal anotherUser = new StompPrincipal(2L, Instant.now().plusSeconds(120));
        StompHeaderAccessor accessor = accessor(StompCommand.SEND, "/pub/auth/reauth", current);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer another-token");
        given(jwtProvider.verifyAccessToken("another-token")).willReturn(anotherUser);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessage("다른 사용자의 토큰으로 재인증할 수 없습니다.");
        assertThat(accessor.getUser()).isSameAs(current);
    }

    private StompHeaderAccessor accessor(
            StompCommand command,
            String destination,
            StompPrincipal principal
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

}
