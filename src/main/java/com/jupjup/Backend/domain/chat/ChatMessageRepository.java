package com.jupjup.Backend.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender WHERE m.chatRoom.id = :chatRoomId ORDER BY m.sentAt ASC")
    List<ChatMessage> findByChatRoomIdOrderBySentAtAsc(@Param("chatRoomId") Long chatRoomId);

    // 특정 채팅방에서 안읽은 메시지 (sender가 다른 사람이 보낸 것)
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
    List<ChatMessage> findUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 사용자가 참여한 모든 채팅방의 안읽은 메시지 수
    @Query("SELECT COUNT(m) FROM ChatMessage m JOIN m.chatRoom r WHERE (r.buyer.id = :userId OR r.seller.id = :userId) AND m.sender.id != :userId AND m.isRead = false")
    long countTotalUnread(@Param("userId") Long userId);

    void deleteByChatRoomId(Long chatRoomId);
}