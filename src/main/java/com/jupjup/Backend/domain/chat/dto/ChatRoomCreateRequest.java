package com.jupjup.Backend.domain.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChatRoomCreateRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;
}