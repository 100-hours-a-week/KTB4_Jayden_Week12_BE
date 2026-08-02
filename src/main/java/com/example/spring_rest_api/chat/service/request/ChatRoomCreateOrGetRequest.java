package com.example.spring_rest_api.chat.service.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomCreateOrGetRequest {
    private Long opponentId;
}
