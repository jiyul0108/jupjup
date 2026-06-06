package com.jupjup.Backend.domain.review;

import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.review.dto.ReportCreateRequest;
import com.jupjup.Backend.domain.review.dto.ReportResponse;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // 신고 접수
    @Transactional
    public ReportResponse create(ReportCreateRequest request, String email) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 본인 상품 신고 불가
        if (product.getSeller().getId().equals(reporter.getId())) {
            throw new IllegalArgumentException("본인 상품은 신고할 수 없습니다.");
        }

        // 중복 신고 방지
        reportRepository.findByReporterIdAndProductId(reporter.getId(), product.getId())
                .ifPresent(r -> { throw new IllegalArgumentException("이미 신고한 상품입니다."); });

        Report report = Report.builder()
                .reporter(reporter)
                .product(product)
                .reason(request.getReason())
                .build();

        return new ReportResponse(reportRepository.save(report));
    }
}