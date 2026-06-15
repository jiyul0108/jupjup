package com.jupjup.Backend.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.buyer JOIN FETCH r.seller JOIN FETCH r.product WHERE r.product.id = :productId AND r.buyer.id = :buyerId")
    Optional<ChatRoom> findByProductIdAndBuyerId(@Param("productId") Long productId, @Param("buyerId") Long buyerId);

    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.buyer JOIN FETCH r.seller JOIN FETCH r.product WHERE r.buyer.id = :userId OR r.seller.id = :userId ORDER BY r.createdAt DESC")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);

    List<ChatRoom> findAllByProductId(Long productId);
}