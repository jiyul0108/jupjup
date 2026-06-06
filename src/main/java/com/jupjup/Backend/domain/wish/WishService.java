package com.jupjup.Backend.domain.wish;

import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import com.jupjup.Backend.domain.wish.dto.WishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishService {

    private final WishRepository wishRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // 찜 토글 (추가 or 취소)
    @Transactional
    public String toggle(Long productId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Optional<Wish> existing = wishRepository.findByUserIdAndProductId(user.getId(), productId);

        if (existing.isPresent()) {
            wishRepository.delete(existing.get());
            return "찜이 취소되었습니다.";
        } else {
            Wish wish = Wish.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishRepository.save(wish);
            return "찜이 추가되었습니다.";
        }
    }

    // 내 찜 목록 조회
    public List<WishResponse> getMyWishes(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return wishRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(WishResponse::new)
                .toList();
    }
}