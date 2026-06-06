package com.jupjup.Backend.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ReportCreateRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotBlank(message = "신고 사유는 필수입니다.")
    private String reason;
}