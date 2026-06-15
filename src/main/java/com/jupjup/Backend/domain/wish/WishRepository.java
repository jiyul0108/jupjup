package com.jupjup.Backend.domain.wish;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    Optional<Wish> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT w FROM Wish w JOIN FETCH w.product WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Wish> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    void deleteAllByProductId(Long productId);
}