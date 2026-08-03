package com.example.spring_rest_api.chat.interceptor;

import com.example.spring_rest_api.authorization.jwt.JwtProvider;
import com.example.spring_rest_api.chat.principal.StompPrincipal;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthenticationInterceptor implements ChannelInterceptor {
    private final JwtProvider jwtProvider;

    private static final String REAUTHENTICATION_DESTINATION = "/pub/auth/reauth";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            StompPrincipal principal = authenticate(accessor);
            accessor.setUser(principal);
            return message;
        }

        if (StompCommand.SEND.equals(command)
                && REAUTHENTICATION_DESTINATION.equals(accessor.getDestination())) {
            reauthenticate(accessor);
            return message;
        }

        if (StompCommand.SEND.equals(command)
                || StompCommand.SUBSCRIBE.equals(command)) {
            validateSessionAuthentication(accessor);
        }

        return message;
    }



    private void reauthenticate(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof StompPrincipal current)) {
            throw new MessageDeliveryException("인증된 WebSocket 세션이 아닙니다.");
        }

        StompPrincipal renewed = authenticate(accessor);

        if (!current.getUserId().equals(renewed.getUserId())) {
            throw new MessageDeliveryException("다른 사용자의 토큰으로 재인증할 수 없습니다.");
        }

        accessor.setUser(renewed);
    }

    private void validateSessionAuthentication(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new MessageDeliveryException("WebSocket 인증이 필요합니다.");
        }

        if (principal.isExpired()) {
            throw new MessageDeliveryException("WebSocket 액세스 토큰이 만료되었습니다.");
        }
    }

    private StompPrincipal authenticate(StompHeaderAccessor accessor) {
        String authorization =
                accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessageDeliveryException("WebSocket 인증 토큰이 없습니다.");
        }

        try {
            return jwtProvider.verifyAccessToken(
                    authorization.substring(7)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new MessageDeliveryException("유효하지 않은 WebSocket 토큰입니다.");
        }
    }
}
