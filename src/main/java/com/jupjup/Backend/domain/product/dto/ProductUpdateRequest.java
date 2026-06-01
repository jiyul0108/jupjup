package com.jupjup.Backend.domain.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {
    private String title;
    private String description;
    private int price;
    private String category;
    private String location;
}