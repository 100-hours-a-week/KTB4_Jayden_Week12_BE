package com.example.spring_rest_api.common.exception;

import com.example.spring_rest_api.common.response.ErrorResponseDto;
import com.example.spring_rest_api.common.response.WebSocketErrorData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketErrorEventListener {
    private static final String ERROR_DESTINATION = "/queue/chat-errors";
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handle(WebSocketErrorEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(event.getSessionId());
        headers.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(
                event.getUsername(),
                ERROR_DESTINATION,
                ErrorResponseDto.of(
                        event.getCode(),
                        WebSocketErrorData.of(event.getRoomId())
                ),
                headers.getMessageHeaders()
        );
    }
}
