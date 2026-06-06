package com.jupjup.Backend.domain.wish;

import com.jupjup.Backend.domain.wish.dto.WishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;

    // 찜 토글 (추가 or 취소)
    @PostMapping("/{productId}")
    public ResponseEntity<String> toggle(
            @PathVariable Long productId,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(wishService.toggle(productId, email));
    }

    // 내 찜 목록 조회
    @GetMapping
    public ResponseEntity<List<WishResponse>> getMyWishes(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(wishService.getMyWishes(email));
    }
}