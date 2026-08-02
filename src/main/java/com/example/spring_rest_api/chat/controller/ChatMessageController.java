package com.example.spring_rest_api.chat.controller;

import com.example.spring_rest_api.chat.service.ChatRoomAuthorizationService;
import com.example.spring_rest_api.chat.service.ChatService;
import com.example.spring_rest_api.chat.service.request.ChatRequest;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomAuthorizationService chatRoomAuthorizationService;

    @MessageMapping("/chatrooms/{roomId}/messages")
    public void sendText(
            @DestinationVariable Long roomId,
            @Payload ChatRequest request,
            Principal principal
    ) {
        Long senderId = Long.valueOf(principal.getName());
        ChatResponse response = chatService.sendText(senderId, roomId, request);

        messagingTemplate.convertAndSend(
                "/sub/chatrooms/" + roomId,
                response
        );
    }
}
