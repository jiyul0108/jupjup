package com.jupjup.Backend.domain.chat.dto;

import com.jupjup.Backend.domain.chat.ChatRoom;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatRoomResponse {

    private final Long id;
    private final Long productId;
    private final String productTitle;
    private final String buyerNickname;
    private final String sellerNickname;
    private final LocalDateTime createdAt;

    public ChatRoomResponse(ChatRoom chatRoom) {
        this.id = chatRoom.getId();
        this.productId = chatRoom.getProduct().getId();
        this.productTitle = chatRoom.getProduct().getTitle();
        this.buyerNickname = chatRoom.getBuyer().getNickname();
        this.sellerNickname = chatRoom.getSeller().getNickname();
        this.createdAt = chatRoom.getCreatedAt();
    }
}