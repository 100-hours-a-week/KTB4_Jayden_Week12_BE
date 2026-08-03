package com.example.spring_rest_api.common.exception;

import com.example.spring_rest_api.common.response.ChatErrorResponse;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(NotFoundException.class)
    @SendToUser(destinations = "/queue/chat-errors", broadcast = false)
    public ChatErrorResponse handleNotFound(NotFoundException e) {
        return ChatErrorResponse.of(
                e.getMessage(),
                "채팅방을 찾을 수 없습니다.",
                null
        );
    }

    @MessageExceptionHandler(MessageDeliveryException.class)
    @SendToUser(destinations = "/queue/chat-errors", broadcast = false)
    public ChatErrorResponse handleMessageDelivery(MessageDeliveryException e) {
        return ChatErrorResponse.of(
                e.getMessage(),
                "메시지 전송 실패",
                null
        );
    }
}
