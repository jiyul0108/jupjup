package com.jupjup.Backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChatMessageRequest {

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;
}