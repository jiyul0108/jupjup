package com.jupjup.Backend.domain.product;

import com.jupjup.Backend.domain.product.dto.ProductCreateRequest;
import com.jupjup.Backend.domain.product.dto.ProductResponse;
import com.jupjup.Backend.domain.product.dto.ProductUpdateRequest;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import com.jupjup.Backend.global.exception.BusinessException;
import com.jupjup.Backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final com.jupjup.Backend.domain.chat.ChatRoomRepository chatRoomRepository;
    private final com.jupjup.Backend.domain.chat.ChatMessageRepository chatMessageRepository;
    private final com.jupjup.Backend.domain.order.OrderRepository orderRepository;
    private final com.jupjup.Backend.domain.wish.WishRepository wishRepository;
    private final com.jupjup.Backend.domain.review.ReportRepository reportRepository;
    private final com.jupjup.Backend.domain.review.ReviewRepository reviewRepository;

    @Transactional
    public ProductResponse create(ProductCreateRequest request, List<MultipartFile> images, String email) throws IOException {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = Product.builder()
                .seller(seller)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .location(request.getLocation())
                .build();

        productRepository.save(product);

        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            imageUrls = imageService.uploadImages(product, images);
        }

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

    public Page<ProductResponse> getList(String keyword, List<String> categories, Integer minPrice, Integer maxPrice, ProductStatus status, Pageable pageable) {
        List<String> cats = (categories == null || categories.isEmpty()) ? null : categories;
        return productRepository.search(keyword, cats, minPrice, maxPrice, status, pageable)
                .map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse getDetail(Long id) {
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.increaseViewCount();
        return ProductResponse.from(product);
    }

    public ProductResponse update(Long id, ProductUpdateRequest request, String email) {
        Product product = productRepository.findByIdWithSeller(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getSeller().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_OWNER);
        }

        product.update(request.getTitle(), request.getDescription(),
                request.getPrice(), request.getCategory(), request.getLocation());

        productRepository.save(product);

        return productRepository.findByIdWithImages(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Transactional
    public void delete(Long id, String email) {
        Product product = productRepository.findByIdWithSeller(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getSeller().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_OWNER);
        }

        // 1. 채팅방의 메시지 먼저 삭제
        List<com.jupjup.Backend.domain.chat.ChatRoom> chatRooms = chatRoomRepository.findAllByProductId(id);
        for (com.jupjup.Backend.domain.chat.ChatRoom room : chatRooms) {
            chatMessageRepository.deleteByChatRoomId(room.getId());
        }
        // 2. 채팅방 삭제
        chatRoomRepository.deleteAll(chatRooms);

        // 3. 주문 삭제
        List<com.jupjup.Backend.domain.order.Order> orders = orderRepository.findAllByProductId(id);
        orderRepository.deleteAll(orders);

        // 4. 찜 삭제
        wishRepository.deleteAllByProductId(id);

        // 5. 신고 삭제
        reportRepository.deleteAllByProductId(id);

        // 6. 리뷰 삭제
        reviewRepository.deleteAllByProductId(id);

        // 7. 상품 삭제
        productRepository.delete(product);
    }

    public ProductResponse updateStatus(Long id, ProductStatus status, String email) {
        Product product = productRepository.findByIdWithSeller(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getSeller().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_OWNER);
        }

        product.updateStatus(status);
        productRepository.save(product);

        return productRepository.findByIdWithImages(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}