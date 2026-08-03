package com.example.spring_rest_api.chat.controller;

import com.example.spring_rest_api.chat.service.ChatService;
import com.example.spring_rest_api.chat.service.request.ChatReadRequest;
import com.example.spring_rest_api.chat.service.request.ChatRequest;
import com.example.spring_rest_api.chat.service.response.ChatReadResponse;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import com.example.spring_rest_api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_DESTINATION = "/sub/chatrooms/";

    @MessageMapping("/chatrooms/{roomId}/messages")
    public void sendText(
            @DestinationVariable Long roomId,
            @Valid @Payload ChatRequest request,
            Principal principal
    ) {
        Long senderId = Long.valueOf(principal.getName());
        ChatResponse response = chatService.sendText(senderId, roomId, request);

        messagingTemplate.convertAndSend(
                "ROOM_DESTINATION" + roomId,
                response
        );

        chatService.publishMessageUpdate(roomId, response);
    }

    @MessageMapping("/chatrooms/{roomId}/read")
    public void readMessage(
            @DestinationVariable Long roomId,
            @Valid @Payload ChatReadRequest request,
            Principal principal
            ) {
        Long readerId = Long.valueOf(principal.getName());
        ChatReadResponse response = chatService.markAsRead(readerId, roomId, request.getLastReadMessageId());

        Optional.ofNullable(response)
                .ifPresent(event -> messagingTemplate.convertAndSend(
                        ROOM_DESTINATION + roomId,
                        ApiResponse.of(
                                "MESSAGE_READ",
                                event
                        )
                ));
    }
}
