package com.example.spring_rest_api.common.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketErrorEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public boolean publish(StompHeaderAccessor accessor, String code, Long roomId) {
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal == null || sessionId == null) {
            return false;
        }

        eventPublisher.publishEvent(new WebSocketErrorEvent(
                principal.getName(),
                sessionId,
                code,
                roomId
        ));
        return true;
    }
}
