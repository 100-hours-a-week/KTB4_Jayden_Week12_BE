package com.example.spring_rest_api.chat.service.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ReAuthResponse {
    private String type;
    private Instant expiresAt;
}
