package com.example.spring_rest_api.chat.principal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class StompPrincipal implements Principal {
    private Long userId;

    private Instant expiresAt;

    @Override
    public String getName() {
        return userId.toString();
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

}
