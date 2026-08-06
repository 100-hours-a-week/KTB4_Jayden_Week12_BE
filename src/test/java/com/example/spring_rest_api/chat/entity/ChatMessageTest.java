package com.example.spring_rest_api.chat.entity;

import com.example.spring_rest_api.chat.service.response.ChatMessagesResponse;
import com.example.spring_rest_api.chat.service.response.ChatResponse;
import com.example.spring_rest_api.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageTest {

    @Test
    void clientMessageIdFromRequestIsReturnedByLiveAndHistoryResponses() {
        ChatRoom room = ChatRoom.createDirect(1L, 2L, "1:2");
        User sender = User.create("sender@example.com", "password", "sender", null);
        String clientMessageId = "52b72a7d-7997-4e11-97b3-87af8057014c";

        ChatMessage message = ChatMessage.createTextMessage(room, sender, clientMessageId, "hello");

        assertThat(message.getClientMessageId()).isEqualTo(clientMessageId);
        assertThat(ChatResponse.from(message).getClientMessageId()).isEqualTo(clientMessageId);
        assertThat(ChatMessagesResponse.from(message).getClientMessageId()).isEqualTo(clientMessageId);
    }
}
