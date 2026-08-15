package com.example.spring_rest_api.common.exception;

import com.example.spring_rest_api.common.response.ErrorResponseDto;
import com.example.spring_rest_api.common.response.WebSocketErrorData;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketErrorSender {
    private final SimpMessagingTemplate messagingTemplate;
    private static final String ERROR_DESTINATION = "/queue/chat-errors";

    public boolean send(StompHeaderAccessor accessor, String code, Long roomId) {
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal == null || sessionId == null) {
            return false;
        }
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                ERROR_DESTINATION,
                ErrorResponseDto.of(
                        code,
                        WebSocketErrorData.of(roomId)
                ),
                headers.getMessageHeaders()
        );
        return true;
    }
}
