package com.example.spring_rest_api.chat.outbox.entity;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    FAILED,
    PUBLISHED,
    DEAD
}
