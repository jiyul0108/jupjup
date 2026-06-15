package com.jupjup.Backend.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface    ProductRepository extends JpaRepository<Product, Long> {

    // 목록 조회 - JOIN FETCH 없이 페이징 (이미지는 별도 조회)
    @Query(value = "SELECT DISTINCT p FROM Product p JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE " +
            "(:keyword IS NULL OR p.title LIKE %:keyword%) AND " +
            "(:#{#categories == null || #categories.isEmpty()} = true OR p.category IN :categories) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:status IS NULL OR p.status = :status)",
            countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                    "(:keyword IS NULL OR p.title LIKE %:keyword%) AND " +
                    "(:#{#categories == null || #categories.isEmpty()} = true OR p.category IN :categories) AND " +
                    "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                    "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                    "(:status IS NULL OR p.status = :status)")
    Page<Product> search(
            @Param("keyword") String keyword,
            @Param("categories") List<String> categories,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    // 이미지 포함 조회 - 상세 페이지용
    @Query("SELECT p FROM Product p JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    // 목록용 이미지 일괄 조회
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p IN :products")
    List<Product> findAllWithImages(@Param("products") List<Product> products);

    @Query("SELECT p FROM Product p JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.seller.id = :sellerId ORDER BY p.createdAt DESC")
    List<Product> findAllBySellerIdWithImages(@Param("sellerId") Long sellerId);

    @Query("SELECT p FROM Product p JOIN FETCH p.seller WHERE p.id = :id")
    Optional<Product> findByIdWithSeller(@Param("id") Long id);
}