package com.jupjup.Backend.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방의 메시지를 시간순으로 조회
    List<ChatMessage> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
}