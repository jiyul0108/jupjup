package com.jupjup.Backend.domain.wish.dto;

import com.jupjup.Backend.domain.wish.Wish;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class WishResponse {

    private final Long id;
    private final Long productId;
    private final String productTitle;
    private final int productPrice;
    private final String productStatus;
    private final LocalDateTime createdAt;

    public WishResponse(Wish wish) {
        this.id = wish.getId();
        this.productId = wish.getProduct().getId();
        this.productTitle = wish.getProduct().getTitle();
        this.productPrice = wish.getProduct().getPrice();
        this.productStatus = wish.getProduct().getStatus().name();
        this.createdAt = wish.getCreatedAt();
    }
}