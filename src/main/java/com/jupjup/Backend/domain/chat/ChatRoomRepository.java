package com.jupjup.Backend.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 특정 상품에 대해 구매자가 이미 생성한 채팅방이 있는지 확인
    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    // 내가 참여한 채팅방 목록 (구매자 또는 판매자)
    @Query("SELECT r FROM ChatRoom r WHERE r.buyer.id = :userId OR r.seller.id = :userId ORDER BY r.createdAt DESC")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);
}