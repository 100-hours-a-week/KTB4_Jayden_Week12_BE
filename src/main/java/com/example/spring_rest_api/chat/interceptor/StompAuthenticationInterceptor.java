package com.example.spring_rest_api.chat.interceptor;

import com.example.spring_rest_api.authorization.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthenticationInterceptor implements ChannelInterceptor {
    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveAccessToken(accessor);

            if (!jwtProvider.isAccessToken(token)) {
                throw new MessageDeliveryException("유효하지 않은 WebSocket 토큰입니다.");
            }
            Long userId = jwtProvider.getUserId(token);

            Authentication authentication = new  UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    List.of()
            );
            accessor.setUser(authentication);
        }
        return message;
    }

    private String resolveAccessToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessageDeliveryException("WebSocket 인증 토큰이 없습니다.");
        }
        return authorization.substring(7);
    }
}
