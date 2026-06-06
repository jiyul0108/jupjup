package com.jupjup.Backend.domain.review;

import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.product.ProductStatus;
import com.jupjup.Backend.domain.review.dto.ReviewCreateRequest;
import com.jupjup.Backend.domain.review.dto.ReviewResponse;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // 리뷰 작성
    @Transactional
    public ReviewResponse create(ReviewCreateRequest request, String email) {
        User reviewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 거래완료 상품만 리뷰 가능
        if (product.getStatus() != ProductStatus.SOLD) {
            throw new IllegalArgumentException("거래완료된 상품만 리뷰를 작성할 수 있습니다.");
        }

        // 판매자 또는 구매자만 리뷰 가능 (판매자가 구매자에게, 또는 구매자가 판매자에게)
        User reviewee;
        if (product.getSeller().getId().equals(reviewer.getId())) {
            // 판매자가 리뷰 작성 → 리뷰 대상은 구매자 (채팅방에서 찾아야 하지만 단순화하여 판매자→구매자 방향은 추후 구현)
            throw new IllegalArgumentException("현재는 구매자만 리뷰를 작성할 수 있습니다.");
        } else {
            // 구매자가 리뷰 작성 → 리뷰 대상은 판매자
            reviewee = product.getSeller();
        }

        // 중복 리뷰 방지
        reviewRepository.findByReviewerIdAndProductId(reviewer.getId(), product.getId())
                .ifPresent(r -> { throw new IllegalArgumentException("이미 리뷰를 작성하셨습니다."); });

        // jupjupScore 반영
        int delta = 0;
        if (request.getScore() >= 4) delta = 1;
        else if (request.getScore() <= 2) delta = -1;
        reviewee.updateJupjupScore(delta);
        userRepository.save(reviewee);

        Review review = Review.builder()
                .reviewer(reviewer)
                .reviewee(reviewee)
                .product(product)
                .content(request.getContent())
                .score(request.getScore())
                .build();

        return new ReviewResponse(reviewRepository.save(review));
    }

    // 특정 유저의 받은 리뷰 목록 조회
    public List<ReviewResponse> getReviews(Long userId) {
        return reviewRepository.findAllByRevieweeIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReviewResponse::new)
                .toList();
    }
}