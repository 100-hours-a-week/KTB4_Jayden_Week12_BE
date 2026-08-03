package com.example.spring_rest_api.chat.controller;

import com.example.spring_rest_api.chat.service.ChatMessageService;
import com.example.spring_rest_api.chat.service.ChatRoomService;
import com.example.spring_rest_api.chat.service.request.ChatRoomCreateOrGetRequest;
import com.example.spring_rest_api.chat.service.response.ChatMessagesResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomCreateOrGetResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomInfoResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomListResponse;
import com.example.spring_rest_api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping("/chatrooms/direct")
    public ResponseEntity<ApiResponse<ChatRoomCreateOrGetResponse>> CreateOrGetChatRoom(
            @Valid @RequestBody ChatRoomCreateOrGetRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        ChatRoomCreateOrGetResponse response = chatRoomService.createOrGetDirectRoom(userId, request);

        return response.isCreated() ?
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.of(
                                "chat_room_created",
                                response
                        )) :
                ResponseEntity.ok(ApiResponse.of(
                        "chat_room_read_success",
                        response
                ));
    }

    @GetMapping("/chatrooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomInfoResponse>> readChatRoomInfo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                "chat_room_info_read_success",
                chatRoomService.readInfo(userId, roomId)
        ));
    }

    @GetMapping("/chatrooms")
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> readChatRoomInfo(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) LocalDateTime createdAtCursor,
            @RequestParam Long pageSize
            ) {
        return ResponseEntity.ok(ApiResponse.of(
                "chat_room_list_read_success",
                chatRoomService.readInfiniteScroll(userId, createdAtCursor, pageSize)
        ));
    }

    @GetMapping("/chatrooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessagesResponse>>> readChatRoomMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam Long pageSize
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                "messages_read_success",
                chatMessageService.readMessages(userId, roomId, lastMessageId, pageSize)
        ));
    }

    @GetMapping("/chatrooms/unread-count")
    public ResponseEntity<ApiResponse<Long>> readUnreadCount(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                "unread_count_load_success",
                chatMessageService.countUnreadMessages(userId)
        ));
    }

    @DeleteMapping("/chatrooms/{roomId}/users/me")
    public ResponseEntity<ApiResponse<Long>> deleteChatRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                "chat_room_delete_success",
                chatRoomService.delete(userId, roomId)
        ));
    }
}
