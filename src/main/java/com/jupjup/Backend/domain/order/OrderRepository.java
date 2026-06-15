package com.jupjup.Backend.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o JOIN FETCH o.product JOIN FETCH o.product.seller JOIN FETCH o.buyer WHERE o.buyer.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findAllByBuyerIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o JOIN FETCH o.product JOIN FETCH o.product.seller JOIN FETCH o.buyer WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    List<Order> findAllByProductId(Long productId);
}
