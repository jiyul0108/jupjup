package com.jupjup.Backend.domain.review.dto;

import com.jupjup.Backend.domain.review.Report;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportResponse {

    private final Long id;
    private final Long productId;
    private final String productTitle;
    private final String reporterNickname;
    private final String reason;
    private final LocalDateTime createdAt;

    public ReportResponse(Report report) {
        this.id = report.getId();
        this.productId = report.getProduct().getId();
        this.productTitle = report.getProduct().getTitle();
        this.reporterNickname = report.getReporter().getNickname();
        this.reason = report.getReason();
        this.createdAt = report.getCreatedAt();
    }
}