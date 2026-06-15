package com.jupjup.Backend.domain.review;

import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.product.ProductStatus;
import com.jupjup.Backend.domain.review.dto.ReviewCreateRequest;
import com.jupjup.Backend.domain.review.dto.ReviewResponse;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import com.jupjup.Backend.global.exception.BusinessException;
import com.jupjup.Backend.global.exception.ErrorCode;
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

    @Transactional
    public ReviewResponse create(ReviewCreateRequest request, String email) {
        User reviewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.SOLD) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        if (product.getSeller().getId().equals(reviewer.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_SELLER_NOT_ALLOWED);
        }

        reviewRepository.findByReviewerIdAndProductId(reviewer.getId(), product.getId())
                .ifPresent(r -> { throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS); });

        User reviewee = product.getSeller();

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

    public List<ReviewResponse> getReviews(Long userId) {
        return reviewRepository.findAllByRevieweeIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReviewResponse::new)
                .toList();
    }
}