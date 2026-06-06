package com.jupjup.Backend.domain.product.dto;

import com.jupjup.Backend.domain.product.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProductUpdateStatusRequest {

    @NotNull(message = "상태값은 필수입니다.")
    private ProductStatus status;
}