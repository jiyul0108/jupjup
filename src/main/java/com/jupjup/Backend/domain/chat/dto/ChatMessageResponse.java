package com.jupjup.Backend.domain.chat.dto;

import com.jupjup.Backend.domain.chat.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {

    private final Long id;
    private final Long chatRoomId;
    private final String senderNickname;
    private final Long senderId;
    private final String content;
    private final boolean isRead;
    private final LocalDateTime sentAt;

    public ChatMessageResponse(ChatMessage message) {
        this.id = message.getId();
        this.chatRoomId = message.getChatRoom().getId();
        this.senderNickname = message.getSender().getNickname();
        this.senderId = message.getSender().getId();
        this.content = message.getContent();
        this.isRead = message.isRead();
        this.sentAt = message.getSentAt();
    }
}