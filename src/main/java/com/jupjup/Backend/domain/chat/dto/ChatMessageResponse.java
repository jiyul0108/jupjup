package com.jupjup.Backend.domain.chat.dto;

import com.jupjup.Backend.domain.chat.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {

    private final Long id;
    private final Long chatRoomId;
    private final String senderNickname;
    private final String content;
    private final LocalDateTime sentAt;

    public ChatMessageResponse(ChatMessage message) {
        this.id = message.getId();
        this.chatRoomId = message.getChatRoom().getId();
        this.senderNickname = message.getSender().getNickname();
        this.content = message.getContent();
        this.sentAt = message.getSentAt();
    }
}