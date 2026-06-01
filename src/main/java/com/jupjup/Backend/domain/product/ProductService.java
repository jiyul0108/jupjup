package com.jupjup.Backend.domain.product;

import com.jupjup.Backend.domain.product.dto.ProductCreateRequest;
import com.jupjup.Backend.domain.product.dto.ProductResponse;
import com.jupjup.Backend.domain.product.dto.ProductUpdateRequest;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    // 상품 등록
    @Transactional
    public ProductResponse create(ProductCreateRequest request, List<MultipartFile> images, String email) throws IOException {
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

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
                product.getPrice(),
                product.getCategory(),
                product.getLocation(),
                product.getStatus().name(),
                product.getViewCount(),
                product.getSeller().getNickname(),
                product.getCreatedAt(),
                imageUrls
        );
    }

    // 상품 목록 조회 (필터 검색)
    public Page<ProductResponse> getList(String category, Integer minPrice, Integer maxPrice, Pageable pageable) {
        return productRepository.search(category, minPrice, maxPrice, pageable)
                .map(ProductResponse::from);
    }

    // 상품 상세 조회
    public ProductResponse getDetail(Long id) {
        Product product = productRepository.findByIdWithImages(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product);
    }

    // 상품 수정
    public ProductResponse update(Long id, ProductUpdateRequest request, String email) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getSeller().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인 상품만 수정할 수 있습니다.");
        }

        product.update(request.getTitle(), request.getDescription(),
                request.getPrice(), request.getCategory(), request.getLocation());

        return ProductResponse.from(productRepository.save(product));
    }

    // 상품 삭제
    public void delete(Long id, String email) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getSeller().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인 상품만 삭제할 수 있습니다.");
        }

        productRepository.delete(product);
    }

    // 거래 상태 변경
    public ProductResponse updateStatus(Long id, ProductStatus status, String email) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getSeller().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인 상품만 상태를 변경할 수 있습니다.");
        }

        product.updateStatus(status);
        return ProductResponse.from(productRepository.save(product));
    }
}