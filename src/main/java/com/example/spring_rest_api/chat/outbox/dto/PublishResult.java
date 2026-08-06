package com.example.spring_rest_api.chat.outbox.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublishResult {
    boolean success;
    int attemptsUsed;
    RuntimeException exception;

    public static PublishResult success(int attemptsUsed) {
        return new PublishResult(
                true,
                attemptsUsed,
                null
        );
    }

    public static PublishResult failure(int attemptsUsed, RuntimeException e) {
        return new PublishResult(
                false,
                attemptsUsed,
                e
        );
    }
}
