package com.example.spring_rest_api.common.exception;

import com.example.spring_rest_api.common.response.ErrorResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class StompErrorHandler extends StompSubProtocolErrorHandler {
    private final ObjectMapper objectMapper;
    private static final String INTERNAL_ERROR_MESSAGE = "WebSocket 메시지 처리 중 오류가 발생했습니다.";

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
            @Nullable Message<byte[]> clientMessage,
            Throwable exception
    ) {
        String message = resolveMessage(exception);
        byte[] payload = serialize(ErrorResponseDto.of(message));

        Message<byte[]> defaultErrorMessage = super.handleClientMessageProcessingError(clientMessage, exception);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                defaultErrorMessage,
                StompHeaderAccessor.class
        );
        if (accessor == null) {
            throw new IllegalStateException("STOMP ERROR 프레임 헤더를 생성할 수 없습니다.");
        }
        if (!accessor.isMutable()) {
            accessor = StompHeaderAccessor.wrap(defaultErrorMessage);
        }

        accessor.setMessage(message);
        accessor.setContentType(MediaType.APPLICATION_JSON);
        accessor.setContentLength(payload.length);

        return MessageBuilder.createMessage(
                payload,
                accessor.getMessageHeaders()
        );
    }




    private String resolveMessage(Throwable exception) {
        String resolvedMessage = null;
        Throwable current = exception;

        while (current != null) {
            if (current instanceof BusinessException || current instanceof MessageDeliveryException) {
                resolvedMessage = current.getMessage();
            }
            if (current == current.getCause()) {
                break;
            }
            current = current.getCause();
        }
        return resolvedMessage;

    }

    private byte[] serialize(ErrorResponseDto response) {
        try {
            return objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException e) {
            return """
                    {"message":"WebSocket 메시지 처리 중 오류가 발생했습니다.","data":null}
                    """.strip().getBytes(StandardCharsets.UTF_8);
        }
    }


}
