package com.example.spring_rest_api.chat.service.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequest {
    @NotNull(message = "clientMessageId는 필수입니다.")
    private String clientMessageId;

    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 1000, message = "최대 1000자까지 작성할 수 있습니다.")
    private String content;
}
