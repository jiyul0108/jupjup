package com.jupjup.Backend.domain.order;

import com.jupjup.Backend.domain.order.dto.OrderCreateRequest;
import com.jupjup.Backend.domain.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestBody @Valid OrderCreateRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(orderService.create(request, email));
    }

    // 내 주문 목록
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(orderService.getMyOrders(email));
    }

    // 주문 상세
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(orderService.getOrder(id, email));
    }
}
