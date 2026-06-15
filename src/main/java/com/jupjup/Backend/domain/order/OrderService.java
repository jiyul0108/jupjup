package com.jupjup.Backend.domain.order;

import com.jupjup.Backend.domain.order.dto.OrderCreateRequest;
import com.jupjup.Backend.domain.order.dto.OrderResponse;
import com.jupjup.Backend.domain.product.Product;
import com.jupjup.Backend.domain.product.ProductRepository;
import com.jupjup.Backend.domain.product.ProductStatus;
import com.jupjup.Backend.domain.user.User;
import com.jupjup.Backend.domain.user.UserRepository;
import com.jupjup.Backend.global.exception.BusinessException;
import com.jupjup.Backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse create(OrderCreateRequest request, String email) {
        User buyer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findByIdWithSeller(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getSeller().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_OWNER);
        }

        if (product.getStatus() == ProductStatus.SOLD) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Order order = Order.builder()
                .buyer(buyer)
                .product(product)
                .buyerName(request.getBuyerName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .paymentMethod(request.getPaymentMethod())
                .request(request.getRequest())
                .build();

        orderRepository.save(order);

        // 상품 상태 거래완료로 변경
        product.updateStatus(ProductStatus.SOLD);
        productRepository.save(product);

        return OrderResponse.from(order);
    }

    public List<OrderResponse> getMyOrders(String email) {
        User buyer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return orderRepository.findAllByBuyerIdOrderByCreatedAtDesc(buyer.getId())
                .stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    public OrderResponse getOrder(Long id, String email) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!order.getBuyer().getEmail().equals(email)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_OWNER);
        }
        return OrderResponse.from(order);
    }
}
