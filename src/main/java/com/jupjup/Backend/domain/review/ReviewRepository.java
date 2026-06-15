package com.jupjup.Backend.domain.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r JOIN FETCH r.reviewer JOIN FETCH r.reviewee JOIN FETCH r.product WHERE r.reviewee.id = :revieweeId ORDER BY r.createdAt DESC")
    List<Review> findAllByRevieweeIdOrderByCreatedAtDesc(@Param("revieweeId") Long revieweeId);

    Optional<Review> findByReviewerIdAndProductId(Long reviewerId, Long productId);

    void deleteAllByProductId(Long productId);
}