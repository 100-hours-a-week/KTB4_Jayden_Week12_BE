package com.example.spring_rest_api.chat.controller;

import com.example.spring_rest_api.chat.service.ChatRoomService;
import com.example.spring_rest_api.chat.service.request.ChatRoomCreateRequest;
import com.example.spring_rest_api.chat.service.response.ChatRoomResponse;
import com.example.spring_rest_api.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping("/chatromms/direct")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> CreateOrGetChatRoom(
            @RequestBody ChatRoomCreateRequest request,
            @AuthenticationPrincipal Long userId

    ) {
        boolean isSaved = chatRoomService.findRoom(userId, request);
        ChatRoomResponse response = isSaved ?
                chatRoomService.getChatRoom(userId, request) :
                chatRoomService.createChatRoom(userId, request);


        return ResponseEntity.ok()
                .body(ApiResponse.of(
                        "chat_room_read_success",
                        response
                ));

    }
}
