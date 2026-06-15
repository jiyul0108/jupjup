package com.jupjup.Backend.domain.review;

import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.review.dto.ReportCreateRequest;
import com.jupjup.Backend.domain.review.dto.ReportResponse;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import com.jupjup.Backend.global.exception.BusinessException;
import com.jupjup.Backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ReportResponse create(ReportCreateRequest request, String email) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getSeller().getId().equals(reporter.getId())) {
            throw new BusinessException(ErrorCode.REPORT_SELF_NOT_ALLOWED);
        }

        reportRepository.findByReporterIdAndProductId(reporter.getId(), product.getId())
                .ifPresent(r -> { throw new BusinessException(ErrorCode.REPORT_ALREADY_EXISTS); });

        Report report = Report.builder()
                .reporter(reporter)
                .product(product)
                .reason(request.getReason())
                .build();

        return new ReportResponse(reportRepository.save(report));
    }
}