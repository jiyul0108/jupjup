package com.jupjup.Backend.domain.review;

import com.jupjup.Backend.domain.review.dto.ReviewCreateRequest;
import com.jupjup.Backend.domain.review.dto.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(reviewService.create(request, email));
    }

    // 특정 유저의 받은 리뷰 목록 조회
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getReviews(userId));
    }
}