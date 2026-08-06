package com.example.spring_rest_api.chat.outbox.dto;

import com.example.spring_rest_api.chat.outbox.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClaimedOutbox {
    private Long outboxId;
    private String eventId;
    private EventType eventType;
    private String channel;
    private String payload;
}
