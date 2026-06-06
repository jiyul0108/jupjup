package com.jupjup.Backend.domain.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 유저가 받은 리뷰 목록
    List<Review> findAllByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    // 같은 상품에 대해 이미 리뷰를 작성했는지 확인 (중복 방지)
    Optional<Review> findByReviewerIdAndProductId(Long reviewerId, Long productId);
}