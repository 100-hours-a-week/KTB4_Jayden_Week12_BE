package com.example.spring_rest_api.chat.interceptor;

import com.example.spring_rest_api.chat.service.ChatRoomAuthorizationService;
import com.example.spring_rest_api.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class StompAuthorizationInterceptor implements ChannelInterceptor {
    private final ChatRoomAuthorizationService chatRoomAuthorizationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscription(accessor);
        }
        return message;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();

        if (principal == null) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/sub/chatrooms/")) {
            return;
        }

        Long userId = Long.valueOf(principal.getName());
        Long chatRoomId = extractChatRoomId(destination);

        chatRoomAuthorizationService.validateParticipant(chatRoomId, userId);

    }

    private Long extractChatRoomId(String destination) {
        String prefix = "/sub/chatrooms/";
        return Long.valueOf(destination.substring(prefix.length()));
    }
}
