package com.example.spring_rest_api.chat.interceptor;

import com.example.spring_rest_api.chat.service.ChatRoomAuthorizationService;
import com.example.spring_rest_api.chat.util.ChatDestinationUtils;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import com.example.spring_rest_api.common.exception.UnauthorizedException;
import com.example.spring_rest_api.common.exception.WebSocketErrorSender;
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
    private final WebSocketErrorSender errorSender;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && validateSubscription(accessor)) {
            return null;
        }

        return message;
    }

    private boolean validateSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();

        if (principal == null) {
            throw new UnauthorizedException("WEBSOCKET_AUTH_REQUIRED");
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/sub/chatrooms/")) {
            return true;
        }

        Long userId = Long.valueOf(principal.getName());
        Long roomId;

        try {
            roomId = ChatDestinationUtils.extractRoomId(destination);
            if (roomId == null) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            errorSender.send(
                    accessor,
                    "INVALID_SUBSCRIPTION_DESTINATION",
                    null
            );
            return false;
        }

        try {
            chatRoomAuthorizationService.validateParticipant(roomId, userId);
            return true;
        } catch (ForbiddenException e) {
            errorSender.send(
                    accessor,
                    "CHAT_ROOM_ACCESS_DENIED",
                    roomId
            );
            return false;
        }
    }
}
