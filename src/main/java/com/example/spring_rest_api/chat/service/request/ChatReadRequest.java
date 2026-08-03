package com.example.spring_rest_api.chat.service.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatReadRequest {
    @NotNull
    @Positive
    private Long lastReadMessageId;

}
