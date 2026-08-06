package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.*;
import com.example.spring_rest_api.chat.event.ChatMessageEvent;
import com.example.spring_rest_api.chat.outbox.entity.ChatOutbox;
import com.example.spring_rest_api.chat.outbox.entity.EventType;
import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.outbox.repository.ChatOutboxRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomMemberRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomRepository;
import com.example.spring_rest_api.chat.service.publisher.ChatUpdatePublisher;
import com.example.spring_rest_api.chat.service.request.ChatRequest;
import com.example.spring_rest_api.chat.service.response.ChatReadResponse;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import com.example.spring_rest_api.chat.service.response.ChatUpdateType;
import com.example.spring_rest_api.common.exception.BadRequestException;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import com.example.spring_rest_api.common.exception.NotFoundException;
import com.example.spring_rest_api.user.entity.User;
import com.example.spring_rest_api.user.repository.UserQueryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserQueryRepository userRepository;
    private final ChatRoomAuthorizationService roomAuthorizationService;
    private final ChatUpdatePublisher updatePublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ChatOutboxRepository outboxRepository;

    @Transactional
    public ChatResponse sendText(Long senderId, Long roomId, ChatRequest request) {
        String clientMessageId = request.getClientMessageId();
        Optional<ChatMessage> messageOptional = messageRepository
                .findBySender_UserIdAndClientMessageId(senderId, clientMessageId);
        if (messageOptional.isPresent()) {
            return ChatResponse.from(messageOptional.get());
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND"));

        roomAuthorizationService.validateParticipant(roomId, senderId);

        User sender = userRepository.findByIdWithProfileImage(senderId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        LocalDateTime now = LocalDateTime.now();
        ChatMessage saved = messageRepository.save(
                ChatMessage.createTextMessage(
                        room,
                        sender,
                        request.getClientMessageId(),
                        request.getContent()
                )
        );
        ChatResponse response = ChatResponse.from(saved);

        ChatMessageEvent event = ChatMessageEvent.of(roomId, response);
        String payload = serializeEvent(event);

        ChatOutbox outbox = ChatOutbox.pending(
                event.getEventId(),
                EventType.CHAT_MESSAGE_CREATED,
                "CHAT_MESSAGE",
                saved.getChatMessageId(),
                saved.getChatMessageId(),
                saved.getClientMessageId(),
                "chat.message.v1",
                payload,
                now
        );
        outboxRepository.save(outbox);

        return response;
    }

    public void publishMessageUpdate(Long roomId, ChatResponse response) {
        List<Long> userIds = memberRepository.findUser_UserIdsByChatRoom_ChatRoomId(roomId);

        userIds.forEach(userId -> {
            Long unreadCount = messageRepository.countUnreadByRoomIdAndUserId(roomId, userId);

            ChatRoomUpdateResponse updateResponse = new ChatRoomUpdateResponse(
                    ChatUpdateType.MESSAGE_RECEIVED,
                    response.getClientMessageId(),
                    roomId,
                    response.getChatMessageId(),
                    response.getUserId(),
                    response.getContent(),
                    ChatType.valueOf(response.getChatType()),
                    response.getCreatedAt(),
                    unreadCount
            );

            updatePublisher.publish(userId, updateResponse);
        });
    }

    @Transactional
    public ChatReadResponse markAsRead(Long readerId, Long roomId, Long lastReadMessageId) {
        ChatRoomMember member = memberRepository.findByChatRoomIdAndUserIdForUpdate(roomId, readerId)
                .filter(m -> m.getLeftAt() == null)
                .orElseThrow(() -> new ForbiddenException("USER_ACCESS_DENIED"));

        ChatMessage message = messageRepository.findByChatMessageIdAndChatRoom_ChatRoomId(lastReadMessageId, roomId)
                .orElseThrow(() -> new NotFoundException("MESSAGE_NOT_FOUND"));

        if (!member.readThrough(message)) {
            return null;
        }

        return ChatReadResponse.updated(roomId, readerId, message.getChatMessageId());
    }




    private String serializeEvent(ChatMessageEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("채팅 메시지 이벤트 직렬화에 실패했습니다. eventId= " + event.getEventId());
        }
    }
}
