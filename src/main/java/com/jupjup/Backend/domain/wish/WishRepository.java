package com.jupjup.Backend.domain.wish;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    // 특정 유저가 특정 상품을 찜했는지 확인
    Optional<Wish> findByUserIdAndProductId(Long userId, Long productId);

    // 내 찜 목록 조회
    List<Wish> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}