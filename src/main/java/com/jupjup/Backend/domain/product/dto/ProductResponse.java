package com.jupjup.Backend.domain.product.dto;

import com.jupjup.Backend.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String title;
    private String description;
    private int price;
    private String category;
    private String location;
    private String status;
    private int viewCount;
    private String sellerNickname;
    private int sellerScore;
    private LocalDateTime createdAt;
    private List<String> imageUrls;

    public static ProductResponse from(Product product) {
        List<String> imageUrls = product.getImages().stream()
                .map(image -> image.getImageUrl())
                .collect(Collectors.toList());

        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getLocation(),
                product.getStatus().name(),
                product.getViewCount(),
                product.getSeller().getNickname(),
                product.getSeller().getJupjupScore(),
                product.getCreatedAt(),
                imageUrls
        );
    }
}