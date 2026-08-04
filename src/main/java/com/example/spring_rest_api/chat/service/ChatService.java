package com.example.spring_rest_api.chat.service;

import com.example.spring_rest_api.chat.entity.ChatMessage;
import com.example.spring_rest_api.chat.entity.ChatRoom;
import com.example.spring_rest_api.chat.entity.ChatRoomMember;
import com.example.spring_rest_api.chat.entity.ChatType;
import com.example.spring_rest_api.chat.publisher.ChatUpdatePublisher;
import com.example.spring_rest_api.chat.repository.ChatMessageRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomMemberRepository;
import com.example.spring_rest_api.chat.repository.ChatRoomRepository;
import com.example.spring_rest_api.chat.service.request.ChatRequest;
import com.example.spring_rest_api.chat.service.response.ChatReadResponse;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import com.example.spring_rest_api.chat.service.response.ChatRoomUpdateResponse;
import com.example.spring_rest_api.chat.service.response.ChatUpdateType;
import com.example.spring_rest_api.common.exception.ForbiddenException;
import com.example.spring_rest_api.common.exception.NotFoundException;
import com.example.spring_rest_api.user.entity.User;
import com.example.spring_rest_api.user.repository.UserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final UserQueryRepository userRepository;
    private final ChatRoomAuthorizationService chatRoomAuthorizationService;
    private final ChatUpdatePublisher updatePublisher;

    @Transactional
    public ChatResponse sendText(Long senderId, Long roomId, ChatRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("ROOM_NOT_FOUND"));

        chatRoomAuthorizationService.validateParticipant(roomId, senderId);

        User user = userRepository.findByIdWithProfileImage(senderId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        ChatMessage textMessage = ChatMessage.createTextMessage(
                room,
                user,
                request.getClientMessageId(),
                request.getContent()
        );
        ChatMessage saved = messageRepository.save(textMessage);
        return ChatResponse.from(saved);
    }

    @Transactional
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
}
