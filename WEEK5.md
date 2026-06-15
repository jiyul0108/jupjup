# 📘 5주차 개발 기록

> **작업 기간**: 5주차  
> **작업 내용**: 추가 기능 개발 및 버그 수정 — 검색/필터, 더보기, 이미지 슬라이더, 구매/주문, 채팅 읽음 처리, 채팅 UI 개선, 마이페이지, 리뷰/신고 UI, AI 설명 자동 작성, 브라우저 탭 설정, 상품 삭제 버그 수정

---

## 1. 버그 수정

### 상품 삭제 500 에러 (외래키 제약)

**문제**: 채팅방, 주문, 리뷰, 신고가 연결된 상품 삭제 시 외래키 제약 오류 발생

```
Cannot delete or update a parent row: a foreign key constraint fails
- orders: FKkp5k52qtiygd8jkag4hayd0qg (product_id)
- chat_rooms: FKo52t6lfonn86xk7t8vapqkniv (product_id)
- reports: FK2wb7diu9f2bue57b2t32g9ac0 (product_id)
```

**원인**: 상품 삭제 전 연관 데이터를 먼저 삭제하지 않음

**해결**: `ProductService.delete()`에서 연관 데이터를 순서대로 삭제

```java
@Transactional
public void delete(Long id, String email) {
    Product product = productRepository.findByIdWithSeller(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

    if (!product.getSeller().getEmail().equals(email)) {
        throw new BusinessException(ErrorCode.PRODUCT_NOT_OWNER);
    }

    // 1. 채팅 메시지 삭제
    List<ChatRoom> chatRooms = chatRoomRepository.findAllByProductId(id);
    for (ChatRoom room : chatRooms) {
        chatMessageRepository.deleteByChatRoomId(room.getId());
    }
    // 2. 채팅방 삭제
    chatRoomRepository.deleteAll(chatRooms);
    // 3. 주문 삭제
    orderRepository.deleteAll(orderRepository.findAllByProductId(id));
    // 4. 찜 삭제
    wishRepository.deleteAllByProductId(id);
    // 5. 신고 삭제
    reportRepository.deleteAllByProductId(id);
    // 6. 리뷰 삭제
    reviewRepository.deleteAllByProductId(id);
    // 7. 상품 삭제
    productRepository.delete(product);
}
```

**추가된 Repository 메서드**

| Repository | 메서드 |
|------------|--------|
| `ChatRoomRepository` | `findAllByProductId(Long productId)` |
| `ChatMessageRepository` | `deleteByChatRoomId(Long chatRoomId)` |
| `OrderRepository` | `findAllByProductId(Long productId)` |
| `WishRepository` | `deleteAllByProductId(Long productId)` |
| `ReportRepository` | `deleteAllByProductId(Long productId)` |
| `ReviewRepository` | `deleteAllByProductId(Long productId)` |

**핵심 학습**: 외래키 참조 순서를 역순으로 삭제해야 한다. 자식 테이블 → 부모 테이블 순서.

---

### OrderCreateRequest @NotBlank → @NotNull

**문제**: 주문 생성 시 500 에러 발생

**원인**: `Long` 타입에 `@NotBlank` 사용 — `@NotBlank`는 `String` 타입에만 적용 가능

```java
// 수정 전
@NotBlank(message = "상품 ID는 필수입니다.")
private Long productId;

// 수정 후
@NotNull(message = "상품 ID는 필수입니다.")
private Long productId;
```

---

## 2. 검색 및 필터 기능

### 백엔드 — 키워드 검색 + 다중 카테고리 + 상태 필터

`ProductRepository.search()` 쿼리를 확장했다.

```java
@Query(value = "SELECT DISTINCT p FROM Product p JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE " +
        "(:keyword IS NULL OR p.title LIKE %:keyword%) AND " +
        "(:#{#categories == null || #categories.isEmpty()} = true OR p.category IN :categories) AND " +
        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
        "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
        "(:status IS NULL OR p.status = :status)",
        countQuery = "SELECT COUNT(p) FROM Product p WHERE ...")
Page<Product> search(
        @Param("keyword") String keyword,
        @Param("categories") List<String> categories,
        @Param("minPrice") Integer minPrice,
        @Param("maxPrice") Integer maxPrice,
        @Param("status") ProductStatus status,
        Pageable pageable
);
```

**핵심 포인트**
- `LIKE %:keyword%` — 제목 부분 일치 검색
- `IN :categories` — 카테고리 복수 선택
- SpEL `(:#{#categories == null || #categories.isEmpty()} = true OR ...)` — categories가 비어있으면 전체 조회
- `countQuery` 분리 — Hibernate `firstResult/maxResults` 경고 방지

### 프론트엔드 — 더보기 버튼

기존 전체 로딩 방식에서 8개씩 페이지 단위로 변경했다.

```jsx
const PAGE_SIZE = 8

// 첫 페이지 로드
useEffect(() => {
    const res = await getProducts({ ...params, page: 0, size: PAGE_SIZE })
    setProducts(res.data.content)
    setHasMore(!res.data.last)
}, [keyword, category, sort, ...])

// 더보기
const handleLoadMore = async () => {
    const nextPage = page + 1
    const res = await getProducts({ ...params, page: nextPage, size: PAGE_SIZE })
    setProducts((prev) => [...prev, ...res.data.content])
    setPage(nextPage)
    setHasMore(!res.data.last)
}
```

- 조건(검색어/필터/정렬) 변경 시 첫 페이지부터 새로 로드
- 더보기 버튼에 현재 표시 수 / 전체 수 표시 (`더보기 (8 / 40)`)
- 전체 로드 완료 시 안내 메시지 표시

---

## 3. 이미지 슬라이더 개선

기존 점(dot) 버튼 방식 → 썸네일 클릭 방식으로 변경

```jsx
function ImageViewer({ images }) {
  const [current, setCurrent] = useState(0)
  const [expanded, setExpanded] = useState(false)

  return (
    <div>
      {/* 메인 이미지 — 클릭하면 확대 */}
      <div className="aspect-video cursor-zoom-in" onClick={() => setExpanded(true)}>
        <img src={images[current]} className="w-full h-full object-cover" />
        <div className="absolute top-2 right-2 ..."> {current + 1} / {images.length} </div>
      </div>

      {/* 썸네일 목록 */}
      {images.length > 1 && (
        <div className="flex gap-2 p-3 overflow-x-auto">
          {images.map((url, i) => (
            <button
              key={i}
              onClick={() => setCurrent(i)}
              style={{ borderColor: i === current ? '#3DDC97' : 'transparent' }}
            >
              <img src={url} className="w-16 h-16 object-cover" />
            </button>
          ))}
        </div>
      )}

      {/* 확대 모달 */}
      {expanded && (
        <div className="fixed inset-0 z-50 bg-black/90" onClick={() => setExpanded(false)}>
          <img src={images[current]} className="max-w-full max-h-full object-contain" />
        </div>
      )}
    </div>
  )
}
```

---

## 4. 줍줍Score 상품 상세 표시

`ProductResponse`에 `sellerScore` 필드를 추가하고 상품 상세 페이지에 표시했다.

```java
// ProductResponse.java
private int sellerScore;

// from() 메서드
product.getSeller().getJupjupScore()
```

---

## 5. 구매 및 주문 기능

### 백엔드 — Order 엔티티

```java
@Entity
@Table(name = "orders")
public class Order {
    private User buyer;
    private Product product;
    private String buyerName;
    private String phone;
    private String address;
    private String paymentMethod;
    private String request;
    private LocalDateTime createdAt;
}
```

**주문 생성 시 동작**
1. 구매자/상품 존재 확인
2. 판매자 본인 구매 방지
3. 이미 거래완료된 상품 구매 방지
4. 주문 저장
5. 상품 상태 자동으로 `SOLD` 처리

```java
product.updateStatus(ProductStatus.SOLD);
productRepository.save(product);
```

### 프론트엔드

- `PurchasePage.jsx` — 구매자 이름, 연락처, 배송지, 결제방법(직거래/계좌이체/카카오페이/네이버페이), 요청사항 입력
- `OrderDetailPage.jsx` — 주문 상세 확인
- 상품 상세에 **구매하기** 버튼 추가 (판매자 본인에게는 표시 안 됨)

---

## 6. 채팅 읽음 처리

### 백엔드

`ChatMessage`에 `isRead` 필드 추가:

```java
@Column(nullable = false)
@Builder.Default
private boolean isRead = false;

public void markAsRead() {
    this.isRead = true;
}
```

채팅방 입장 시 자동 읽음 처리:

```java
// ChatService.getMessages()
List<ChatMessage> unread = chatMessageRepository.findUnreadMessages(roomId, user.getId());
unread.forEach(ChatMessage::markAsRead);
chatMessageRepository.saveAll(unread);
```

전체 안읽은 메시지 수 조회:

```java
@Query("SELECT COUNT(m) FROM ChatMessage m JOIN m.chatRoom r WHERE (r.buyer.id = :userId OR r.seller.id = :userId) AND m.sender.id != :userId AND m.isRead = false")
long countTotalUnread(@Param("userId") Long userId);
```

### 프론트엔드

- **헤더**: 로그인 시 `GET /api/chat/unread` 호출 → 채팅 버튼에 빨간 뱃지
- **채팅 목록**: 채팅방별 `unreadCount` 뱃지
- **채팅방 내**: 내 메시지 옆에 `읽음` (초록) / `1` (검정) 표시

```jsx
{isMine && (
  <span style={{ color: msg.isRead ? '#3DDC97' : '#374151' }}>
    {msg.isRead ? '읽음' : '1'}
  </span>
)}
```

---

## 7. 채팅 UI 개선

채팅 목록과 채팅방 모두 전체 화면을 꽉 채우는 레이아웃에서 **가운데 박스 스타일**로 변경했다.

```jsx
// 채팅 목록
<div className="flex justify-center px-4 py-6">
  <div className="w-full max-w-xl bg-white rounded-2xl shadow-sm overflow-hidden">
    ...
  </div>
</div>

// 채팅방
<div className="flex-1 flex items-start justify-center px-4 py-6">
  <div className="w-full max-w-xl bg-white rounded-2xl shadow-sm flex flex-col"
       style={{ height: 'calc(100vh - 120px)' }}>
    ...
    {/* 메시지 영역 배경색 구분 */}
    <div className="flex-1 overflow-y-auto" style={{ background: '#F9FAFB' }}>
      ...
    </div>
    ...
  </div>
</div>
```

- 메시지 영역 배경을 연한 회색(`#F9FAFB`)으로 줘서 말풍선과 구분
- 채팅창 높이는 `calc(100vh - 120px)`로 화면에 맞게 자동 조절

---

## 8. 마이페이지

6개 탭으로 구성된 마이페이지를 구현했다.

| 탭 | 내용 |
|----|------|
| 프로필 | 닉네임, 이메일, 동네, 줍줍Score + 배지 + 판매/구매/리뷰 수 |
| 내 판매 | 내가 등록한 상품 목록 (거래 상태 표시) |
| 내 구매 | 주문 내역 목록 + **리뷰 작성** 버튼 |
| 찜 목록 | WishListPage로 이동 |
| 받은 리뷰 | 리뷰 목록 + 별점 |
| 정보 수정 | 닉네임, 동네, 비밀번호 변경 |

**줍줍Score 배지 시스템**:

```js
function getScoreBadge(score) {
  if (score >= 50) return { icon: '🌳', label: '나무', color: '#1D9E75' }
  if (score >= 20) return { icon: '🌿', label: '새싹', color: '#3DDC97' }
  return { icon: '🌱', label: '씨앗', color: '#6EE7B7' }
}
```

**백엔드 신규 API**:

```
GET  /api/auth/me              // 프로필 조회
PUT  /api/auth/me              // 프로필 수정 (닉네임, 동네, 비밀번호)
GET  /api/auth/me/products     // 내 판매 상품 목록
```

---

## 9. 리뷰 작성 UI

마이페이지 내 구매 탭의 각 주문에 **⭐ 리뷰 작성** 버튼을 추가했다.

```jsx
<button
  onClick={() => {
    setReviewModal({ productId: o.productId, productTitle: o.productTitle })
    setReviewForm({ content: '', score: 5 })
  }}
  className="text-xs px-3 py-1 rounded-full text-white font-medium"
  style={{ background: '#3DDC97' }}
>
  ⭐ 리뷰 작성
</button>
```

모달에서 별점(1~5) + 내용 작성 후 제출하면 줍줍Score에 자동 반영된다.

---

## 10. 신고 UI

상품 상세 페이지에서 구매자에게만 **🚨 신고하기** 버튼을 표시했다.

```jsx
{/* 판매자 본인에게는 표시 안 됨 */}
{!isMine && user && (
  <div className="mt-3 flex justify-end">
    <button
      onClick={() => setShowReportModal(true)}
      className="text-xs text-gray-400 hover:text-red-400"
    >
      🚨 신고하기
    </button>
  </div>
)}
```

모달에서 신고 사유를 입력 후 접수하면 `POST /api/reports`로 전송된다.

---

## 11. AI 상품 설명 자동 작성

### 구현 방식

```
프론트 → Spring Boot(/api/ai/description) → Gemini API → 응답 반환
```

API 키를 백엔드 `application.properties`에 보관하여 프론트에 노출되지 않게 했다.

### 백엔드

```java
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @PostMapping("/description")
    public ResponseEntity<Map<String, String>> generateDescription(@RequestBody AiDescriptionRequest request) {
        String prompt = String.format(
            "상품명: %s\n카테고리: %s\n판매자 메모: %s\n\n" +
            "중고거래 판매 설명을 한국어로 3~5문장 작성해주세요. 직거래 환영 멘트 포함.",
            request.getTitle(), request.getCategory(), request.getMemo()
        );
        // RestTemplate으로 Gemini API 호출 후 description 반환
    }
}
```

### 프론트엔드 — 모달 UI

**✨ AI 설명 작성** 버튼 클릭 시 모달이 열린다:
- 상품명 / 카테고리 자동 표시 (읽기 전용)
- 간단 메모 입력 (선택) — 상태, 특징 등 추가 정보
- 설명 생성 버튼 → 백엔드 경유 Gemini 호출 → textarea에 결과 자동 입력

---

## 12. 브라우저 탭 설정

### 타이틀 변경

```html
<!-- index.html -->
<title>줍줍 - 중고거래</title>
```

### 파비콘 적용

헤더 로고(초록 배경 + 흰색 '줍' 텍스트)와 동일한 디자인을 SVG로 제작했다.

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32">
  <rect width="32" height="32" rx="8" fill="#3DDC97"/>
  <text x="16" y="23" font-family="Arial" font-size="18" font-weight="900"
        fill="white" text-anchor="middle">줍</text>
</svg>
```

파일 경로: `frontend/public/favicon.svg`

---

## 13. 보안 강화 — .gitignore 설정

`application.properties`에 DB 비밀번호, JWT 시크릿, Gemini API 키가 포함되어 있어 GitHub에 올라가지 않도록 설정했다.

```
# Backend/.gitignore 추가
src/main/resources/application.properties
uploads/
```

대신 `application.properties.example` 파일을 만들어 빈 템플릿만 커밋했다.

---

## 💡 이번 주 핵심 학습

1. **외래키 삭제 순서**: 자식 테이블 먼저, 부모 테이블 나중. 연관 테이블이 많을수록 순서를 꼼꼼히 확인해야 한다.

2. **`@NotBlank` vs `@NotNull`**: `@NotBlank`는 `String`에만 사용 가능. `Long`, `Integer` 등 숫자 타입은 `@NotNull` 사용.

3. **카테고리 복수 선택과 JPA IN 절**: `List<String>` 파라미터를 `IN` 절에 사용할 때 SpEL로 null/empty 체크 필요.
   ```java
   :#{#categories == null || #categories.isEmpty()} = true OR p.category IN :categories
   ```

4. **더보기 페이지네이션**: 조건이 바뀌면 `page=0`부터 새로 시작하고, 더보기 시에는 기존 데이터에 append해야 한다.

5. **AI API 보안**: 프론트에서 직접 외부 API를 호출하면 API 키가 노출됨. 백엔드를 프록시로 두어 키를 서버에서만 관리.

6. **React Fragment (`<>...</>`)**: 모달처럼 최상위 요소 외부에 렌더링할 요소가 있을 때 Fragment로 감싸면 불필요한 wrapper div가 생기지 않음.

---

## ✅ 5주차 완료 체크리스트

- [x] 상품 삭제 버그 수정 (외래키 연관 데이터 순서 삭제)
- [x] OrderCreateRequest `@NotBlank` → `@NotNull` 수정
- [x] 검색 기능 (키워드 + 카테고리 복수 선택 + 가격/상태 필터)
- [x] 더보기 버튼 (8개씩 페이지네이션)
- [x] 이미지 슬라이더 개선 (썸네일 + 확대 모달)
- [x] 줍줍Score 상품 상세 표시
- [x] 구매/주문 기능 (PurchasePage, OrderDetailPage)
- [x] 채팅 읽음 처리 (isRead, 뱃지, 읽음/1 표시)
- [x] 채팅 UI 개선 (가운데 박스 스타일)
- [x] 마이페이지 (6개 탭)
- [x] 리뷰 작성 UI (별점 + 내용 모달)
- [x] 신고 UI (상품 상세 모달)
- [x] AI 상품 설명 자동 작성 (Gemini API 백엔드 경유)
- [x] 브라우저 탭 타이틀 + 파비콘 설정
- [x] application.properties .gitignore 처리
