package com.jupjup.Backend.domain.product;

import com.jupjup.Backend.domain.product.dto.ProductCreateRequest;
import com.jupjup.Backend.domain.product.dto.ProductResponse;
import com.jupjup.Backend.domain.product.dto.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import com.jupjup.Backend.domain.product.ProductStatus;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> create(
            @RequestPart("data") @Valid ProductCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal String email) throws IOException {
        return ResponseEntity.ok(productService.create(request, images, email));
    }

    // 상품 목록 조회
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(productService.getList(category, minPrice, maxPrice, pageable));
    }
    // 상품 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getDetail(id));
    }

    // 상품 수정
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(productService.update(id, request, email));
    }

    // 상품 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        productService.delete(id, email);
        return ResponseEntity.noContent().build();
    }
    // 거래 상태 변경
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ProductStatus status,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(productService.updateStatus(id, status, email));
    }


}