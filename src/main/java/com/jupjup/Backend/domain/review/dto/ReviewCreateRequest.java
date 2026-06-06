package com.jupjup.Backend.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ReviewCreateRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    private String content;

    @NotNull(message = "점수는 필수입니다.")
    @Min(value = 1, message = "점수는 최소 1점입니다.")
    @Max(value = 5, message = "점수는 최대 5점입니다.")
    private Integer score;
}