package com.example.spring_rest_api.common.exception;

import com.example.spring_rest_api.chat.util.ChatDestinationUtils;
import com.example.spring_rest_api.common.response.ErrorResponseDto;
import com.example.spring_rest_api.common.response.WebSocketErrorData;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {
    private static final String ERROR_DESTINATION = "/queue/chat-errors";

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ErrorResponseDto handleBusiness(BusinessException e, Message<?> message) {
        return createErrorResponse(
                e.getMessage(),
                message
        );
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ErrorResponseDto handleNotValid(MethodArgumentNotValidException e, Message<?> message) {
        return createErrorResponse(
                "요청 값이 올바르지 않습니다.",
                message
        );
    }

    @MessageExceptionHandler(MessageConversionException.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ErrorResponseDto handleMessageConversion(MessageConversionException e, Message<?> message) {
        return createErrorResponse(
                "요청 메시지 형식이 올바르지 않습니다.",
                message
        );
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ErrorResponseDto handleException(Exception e, Message<?> message) {
        return createErrorResponse(
                "서버 내부 오류가 발생했습니다.",
                message
        );
    }




    private ErrorResponseDto createErrorResponse(String code, Message<?> message) {
        String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
        Long roomId = ChatDestinationUtils.extractRoomId(destination);

        return ErrorResponseDto.of(
                code,
                WebSocketErrorData.of(roomId)
        );
    }
}
