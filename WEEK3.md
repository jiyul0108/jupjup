# 줍줍 (JupJup) — 3주차 개발 로그

## 개요

3주차에는 WebSocket 실시간 채팅, 찜하기, 리뷰 + 줍줍스코어 반영, 신고 기능을 구현했습니다. 또한 기존 거래 상태 변경 API의 `@RequestParam` → `@RequestBody` 개선 및 코드 품질 개선 작업도 병행했습니다.

---

## 구현 기능 목록

| 기능 | 메서드 | 엔드포인트 | 설명 |
|------|--------|------------|------|
| 채팅방 생성 | POST | `/api/chat/rooms` | 이미 존재하면 기존 채팅방 반환 |
| 내 채팅방 목록 | GET | `/api/chat/rooms` | 구매자/판매자 모두 조회 |
| 이전 메시지 조회 | GET | `/api/chat/rooms/{roomId}/messages` | 시간순 정렬 |
| 실시간 메시지 전송 | STOMP | `/pub/chat/{roomId}` | WebSocket 실시간 채팅 |
| 찜 토글 | POST | `/api/wishes/{productId}` | 찜 추가/취소 토글 방식 |
| 내 찜 목록 | GET | `/api/wishes` | 최신순 정렬 |
| 리뷰 작성 | POST | `/api/reviews` | 거래완료 상품만 가능, 중복 방지 |
| 리뷰 목록 조회 | GET | `/api/reviews/users/{userId}` | 특정 유저의 받은 리뷰 |
| 신고 접수 | POST | `/api/reports` | 본인 상품 신고 불가, 중복 방지 |

---

## Stage 1 — WebSocket 실시간 채팅

### 신규 파일

```
global/config/WebSocketConfig.java
global/jwt/JwtHandshakeInterceptor.java
domain/chat/ChatRoomRepository.java
domain/chat/ChatMessageRepository.java
domain/chat/dto/ChatRoomCreateRequest.java
domain/chat/dto/ChatRoomResponse.java
domain/chat/dto/ChatMessageRequest.java
domain/chat/dto/ChatMessageResponse.java
domain/chat/ChatService.java
domain/chat/ChatController.java
domain/chat/ChatWebSocketController.java
```

### WebSocketConfig.java

STOMP 엔드포인트 및 메시지 브로커를 설정합니다.

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

- `/sub` : 클라이언트가 메시지를 구독하는 prefix
- `/pub` : 클라이언트가 메시지를 발행하는 prefix
- SockJS 폴백을 활성화하여 WebSocket을 지원하지 않는 환경에서도 동작

### JwtHandshakeInterceptor.java

WebSocket 연결(핸드셰이크) 시 쿼리 파라미터로 전달된 JWT 토큰을 검증하고, 인증된 이메일을 세션 속성에 저장합니다.

```java
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "token".equals(kv[0])) {
                    String token = kv[1];
                    if (jwtUtil.validateToken(token)) {
                        attributes.put("email", jwtUtil.getEmailFromToken(token));
                        return true;
                    }
                }
            }
        }
        return false;
    }
    // ...
}
```

클라이언트는 WebSocket 연결 시 `ws://localhost:8080/ws?token={JWT}` 형식으로 토큰을 전달합니다.

### ChatWebSocketController.java

STOMP 메시지를 처리하는 컨트롤러입니다. `Principal` 대신 `SimpMessageHeaderAccessor`로 세션 속성에서 이메일을 꺼내는 방식을 사용합니다.

```java
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String email = (String) headerAccessor.getSessionAttributes().get("email");
        ChatMessageResponse response = chatService.saveMessage(roomId, request, email);
        messagingTemplate.convertAndSend("/sub/chat/" + roomId, response);
    }
}
```

> **트러블슈팅**: `Principal principal`을 파라미터로 받으면 WebSocket 환경에서 null이 되는 문제가 있었습니다. 핸드셰이크 시 세션 속성에 저장한 이메일을 `SimpMessageHeaderAccessor`로 꺼내는 방식으로 해결했습니다.

### ChatRoomRepository.java

```java
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    @Query("SELECT r FROM ChatRoom r WHERE r.buyer.id = :userId OR r.seller.id = :userId ORDER BY r.createdAt DESC")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);
}
```

### SecurityConfig.java 수정

WebSocket 엔드포인트에 대한 인증 예외를 추가했습니다.

```java
.requestMatchers("/ws/**").permitAll()
```

---

## Stage 2 — 찜하기

### 신규 파일

```
domain/wish/WishRepository.java
domain/wish/dto/WishResponse.java
domain/wish/WishService.java
domain/wish/WishController.java
```

### 찜 토글 로직

같은 상품을 이미 찜한 경우 취소, 아닌 경우 추가하는 토글 방식으로 구현했습니다.

```java
@Transactional
public String toggle(Long productId, String email) {
    // ...
    Optional<Wish> existing = wishRepository.findByUserIdAndProductId(user.getId(), productId);

    if (existing.isPresent()) {
        wishRepository.delete(existing.get());
        return "찜이 취소되었습니다.";
    } else {
        Wish wish = Wish.builder().user(user).product(product).build();
        wishRepository.save(wish);
        return "찜이 추가되었습니다.";
    }
}
```

---

## Stage 3 — 리뷰 + 줍줍스코어 반영

### 신규 파일

```
domain/review/ReviewRepository.java
domain/review/dto/ReviewCreateRequest.java
domain/review/dto/ReviewResponse.java
domain/review/ReviewService.java
domain/review/ReviewController.java
```

### 줍줍스코어 반영 로직

리뷰 점수에 따라 판매자의 줍줍스코어를 조정합니다.

```java
int delta = 0;
if (request.getScore() >= 4) delta = 1;
else if (request.getScore() <= 2) delta = -1;
reviewee.updateJupjupScore(delta);
userRepository.save(reviewee);
```

| 점수 | 줍줍스코어 변화 |
|------|----------------|
| 4 ~ 5점 | +1 |
| 3점 | 변동 없음 |
| 1 ~ 2점 | -1 |

### 리뷰 제약 조건

- 거래완료(`SOLD`) 상태의 상품만 리뷰 작성 가능
- 같은 상품에 중복 리뷰 작성 불가
- 현재는 구매자 → 판매자 방향만 지원

### User.java 수정

줍줍스코어 업데이트 메서드를 추가했습니다.

```java
public void updateJupjupScore(int delta) {
    this.jupjupScore += delta;
}
```

---

## Stage 4 — 신고

### 신규 파일

```
domain/review/ReportRepository.java
domain/review/dto/ReportCreateRequest.java
domain/review/dto/ReportResponse.java
domain/review/ReportService.java
domain/review/ReportController.java
```

### 신고 제약 조건

- 본인 상품 신고 불가
- 같은 상품에 중복 신고 불가

```java
// 본인 상품 신고 불가
if (product.getSeller().getId().equals(reporter.getId())) {
    throw new IllegalArgumentException("본인 상품은 신고할 수 없습니다.");
}

// 중복 신고 방지
reportRepository.findByReporterIdAndProductId(reporter.getId(), product.getId())
        .ifPresent(r -> { throw new IllegalArgumentException("이미 신고한 상품입니다."); });
```

---

## 기존 코드 개선

### 거래 상태 변경 API `@RequestParam` → `@RequestBody` 변경

기존에 쿼리 파라미터(`?status=SOLD`)로 받던 방식을 `@RequestBody`로 통일했습니다.

`ProductUpdateStatusRequest.java` DTO를 새로 추가했습니다.

```java
@Getter
public class ProductUpdateStatusRequest {
    @NotNull(message = "상태값은 필수입니다.")
    private ProductStatus status;
}
```

**변경 전:**
```
PATCH /api/products/1/status?status=SOLD
```

**변경 후:**
```json
PATCH /api/products/1/status
{ "status": "SOLD" }
```

### application.properties 개선

`open-in-view` 경고를 제거했습니다.

```properties
spring.jpa.open-in-view=false
```

---

## 트러블슈팅 정리

| 문제 | 원인 | 해결 |
|------|------|------|
| WebSocket 메시지 전송 시 `NullPointerException` | `Principal` 파라미터가 WebSocket 환경에서 null | `SimpMessageHeaderAccessor`로 세션 속성에서 이메일 직접 추출 |
| 채팅방 생성 500 에러 | 판매자 본인 계정으로 채팅 시도 | 의도된 동작 (본인 상품 채팅 불가 검증 로직) |
| 거래 상태 변경 400 에러 | `@RequestParam`으로 구현된 API에 Body로 요청 | `@RequestBody` 방식으로 변경 |

---

## 4주차 예정 작업

- React + Tailwind CSS + shadcn/ui 프론트엔드 구현
- 예외 처리 통일 (`@RestControllerAdvice`)
- 전체 API 연동 및 UI 완성
