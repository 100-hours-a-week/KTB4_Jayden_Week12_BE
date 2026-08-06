package com.example.spring_rest_api.chat.outbox.entity;

public enum EventType {
    CHAT_MESSAGE_CREATED,
    CHAT_MESSAGE_UPDATED,
    CHAT_MESSAGE_DELETED
}
