package com.jupjup.Backend.domain.order.dto;

import com.jupjup.Backend.domain.order.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long productId;
    private String productTitle;
    private int productPrice;
    private String sellerNickname;
    private String buyerName;
    private String phone;
    private String address;
    private String paymentMethod;
    private String request;
    private LocalDateTime createdAt;

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProduct().getId(),
                order.getProduct().getTitle(),
                order.getProduct().getPrice(),
                order.getProduct().getSeller().getNickname(),
                order.getBuyerName(),
                order.getPhone(),
                order.getAddress(),
                order.getPaymentMethod(),
                order.getRequest(),
                order.getCreatedAt()
        );
    }
}
