package com.jupjup.Backend.domain.wish;

import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import com.jupjup.Backend.domain.wish.dto.WishResponse;
import com.jupjup.Backend.global.exception.BusinessException;
import com.jupjup.Backend.global.exception.ErrorCode;
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

    @Transactional
    public String toggle(Long productId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        Optional<Wish> existing = wishRepository.findByUserIdAndProductId(user.getId(), productId);

        if (existing.isPresent()) {
            wishRepository.delete(existing.get());
            return "찜이 취소되었습니다.";
        } else {
            wishRepository.save(Wish.builder().user(user).product(product).build());
            return "찜이 추가되었습니다.";
        }
    }

    public List<WishResponse> getMyWishes(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return wishRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(WishResponse::new)
                .toList();
    }
}