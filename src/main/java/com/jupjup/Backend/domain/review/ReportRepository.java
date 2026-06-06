package com.jupjup.Backend.domain.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 같은 상품에 대해 이미 신고했는지 확인 (중복 방지)
    Optional<Report> findByReporterIdAndProductId(Long reporterId, Long productId);
}