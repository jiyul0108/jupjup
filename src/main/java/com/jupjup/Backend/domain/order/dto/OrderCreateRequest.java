package com.jupjup.Backend.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotBlank(message = "구매자 이름은 필수입니다.")
    private String buyerName;

    @NotBlank(message = "연락처는 필수입니다.")
    private String phone;

    @NotBlank(message = "배송지 주소는 필수입니다.")
    private String address;

    @NotBlank(message = "결제 방법은 필수입니다.")
    private String paymentMethod;

    private String request;
}
