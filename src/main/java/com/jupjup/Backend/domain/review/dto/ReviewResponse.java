package com.jupjup.Backend.domain.review.dto;

import com.jupjup.Backend.domain.review.Review;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReviewResponse {

    private final Long id;
    private final Long productId;
    private final String productTitle;
    private final String reviewerNickname;
    private final String revieweeNickname;
    private final String content;
    private final int score;
    private final LocalDateTime createdAt;

    public ReviewResponse(Review review) {
        this.id = review.getId();
        this.productId = review.getProduct().getId();
        this.productTitle = review.getProduct().getTitle();
        this.reviewerNickname = review.getReviewer().getNickname();
        this.revieweeNickname = review.getReviewee().getNickname();
        this.content = review.getContent();
        this.score = review.getScore();
        this.createdAt = review.getCreatedAt();
    }
}