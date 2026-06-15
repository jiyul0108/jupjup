# 5주차 개발 일지

> 추가 기능 개발 및 버그 수정 — 검색/필터, 이미지 슬라이더, 구매/주문, 채팅 읽음 처리, 마이페이지, AI 설명 자동 작성

---

## 1. 버그 수정

### 이미지 중복 업로드 문제

**문제**: 상품 등록 시 이미지를 한 장 추가한 뒤 또 추가하면 기존 이미지가 사라지고 새 이미지만 남는 문제

**원인**: `handleImageChange`에서 새 파일을 선택할 때마다 기존 이미지 전체를 `createObjectURL`로 다시 변환해 기존 미리보기 URL이 깨짐

**수정 전**:
```js
const newPreviews = newImages.map((f) => URL.createObjectURL(f))
```

**수정 후**:
```js
const combined = [...images, ...files].slice(0, 5)
const newPreviews = [
  ...previews.slice(0, images.length),   // 기존 미리보기 URL 그대로 유지
  ...files.map((f) => URL.createObjectURL(f))  // 새 파일만 URL 생성
].slice(0, 5)
```

**핵심**: 기존 미리보기는 이미 유효한 URL이므로 다시 생성하지 않고 유지한다.

---

### OrderCreateRequest @NotBlank → @NotNull

**문제**: 주문 생성 시 500 에러 발생

**원인**: `Long` 타입 필드에 `@NotBlank` 사용 — `@NotBlank`는 `String` 타입에만 적용 가능

```
HV000030: No validator could be found for constraint 'NotBlank' validating type 'Long'
```

**수정**:
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

`ProductRepository`의 `search` 쿼리를 확장했다.

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

**주요 포인트**:
- `LIKE %:keyword%` — 제목 부분 일치 검색
- `IN :categories` — 카테고리 복수 선택 (`List<String>`)
- `SpEL 표현식` `(:#{#categories == null || #categories.isEmpty()} = true OR ...)` — categories가 비어있으면 전체 조회
- `countQuery` 분리 — collection fetch와 Pageable을 함께 쓸 때 메모리 경고 방지

### 프론트엔드 — 검색 후 필터 버튼 표시

```jsx
// 검색 중일 때만 필터 버튼 표시
const isSearching = !!keyword

{isSearching && (
  <button onClick={() => setShowFilter(!showFilter)}>
    🔍 필터{hasFilter ? ' 적용중' : ''}
  </button>
)}
```

- `Header.jsx`: 검색창에서 Enter 또는 버튼으로 검색 → URL에 `?keyword=...` 쿼리 파라미터 추가
- `MainPage.jsx`: `useSearchParams`로 URL 파라미터 읽기
- 필터 패널: 가격 범위, 카테고리(복수 선택), 거래 상태 필터 포함
- 카테고리 복수 선택은 URL에 콤마로 구분해 저장 후 배열로 변환하여 API 요청

---

## 3. 이미지 슬라이더 개선

기존 점(dot) 버튼 방식에서 번개장터 스타일의 썸네일 방식으로 변경했다.

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

```jsx
// ProductDetailPage.jsx
<div className="flex items-center gap-1 mt-0.5">
  <span className="text-xs text-gray-400">줍줍Score</span>
  <span className="text-xs font-bold" style={{ color: '#1D9E75' }}>
    {product.sellerScore ?? 0}
  </span>
</div>
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

**주문 생성 시 동작**:
1. 구매자/상품 존재 확인
2. 판매자 본인 구매 방지
3. 이미 거래완료된 상품 구매 방지
4. 주문 저장
5. 상품 상태 자동으로 `SOLD` 처리

```java
// 상품 상태 자동 변경
product.updateStatus(ProductStatus.SOLD);
productRepository.save(product);
```

### 프론트엔드 — 구매 페이지

- 결제 방법: 직거래, 계좌이체, 카카오페이, 네이버페이 선택
- 주문 완료 후 주문 상세 페이지로 이동
- 상품 상세 페이지에 **구매하기** 버튼 추가 (판매자 본인에게는 표시 안 됨)

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

안읽은 메시지 조회 쿼리:

```java
// 특정 채팅방에서 내가 받은 안읽은 메시지
@Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
List<ChatMessage> findUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);

// 전체 안읽은 메시지 수
@Query("SELECT COUNT(m) FROM ChatMessage m JOIN m.chatRoom r WHERE (r.buyer.id = :userId OR r.seller.id = :userId) AND m.sender.id != :userId AND m.isRead = false")
long countTotalUnread(@Param("userId") Long userId);
```

채팅방 입장 시 자동 읽음 처리:

```java
// ChatService.getMessages()
List<ChatMessage> unread = chatMessageRepository.findUnreadMessages(roomId, user.getId());
unread.forEach(ChatMessage::markAsRead);
chatMessageRepository.saveAll(unread);
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

## 7. 마이페이지

6개 탭으로 구성된 마이페이지를 구현했다.

| 탭 | 내용 |
|----|------|
| 프로필 | 닉네임, 이메일, 동네, 줍줍Score + 배지 + 판매/구매/리뷰 수 |
| 내 판매 | 내가 등록한 상품 목록 (거래 상태 표시) |
| 내 구매 | 주문 내역 목록 |
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

```java
GET  /api/auth/me              // 프로필 조회
PUT  /api/auth/me              // 프로필 수정 (닉네임, 동네, 비밀번호)
GET  /api/auth/me/products     // 내 판매 상품 목록
```

---

## 8. AI 상품 설명 자동 작성

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

        // RestTemplate으로 Gemini API 호출
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
        // ... 응답 파싱 후 반환
    }
}
```

### 프론트엔드 — 모달 UI

✨ AI 설명 작성 버튼 클릭 시 모달이 열린다:
- 상품명 / 카테고리 자동 표시 (읽기 전용)
- 간단 메모 입력 (선택) — 상태, 특징 등 추가 정보
- 설명 생성 버튼 클릭 → 백엔드 경유 Gemini 호출 → textarea에 결과 자동 입력

---

## 9. 보안 강화 — .gitignore 설정

`application.properties`에 DB 비밀번호, JWT 시크릿, Gemini API 키가 포함되어 있어 GitHub에 올라가지 않도록 설정했다.

```
# Backend/.gitignore 추가
src/main/resources/application.properties
uploads/
```

대신 `application.properties.example` 파일을 만들어 빈 템플릿만 커밋했다.

---

## 💡 이번 주 핵심 학습

1. **`@NotBlank` vs `@NotNull`**: `@NotBlank`는 `String`에만 사용 가능. `Long`, `Integer` 등 숫자 타입은 `@NotNull` 사용

2. **카테고리 복수 선택과 JPA IN 절**: Spring Data JPA에서 `List<String>` 파라미터를 `IN` 절에 사용할 때 SpEL로 null/empty 체크 필요
   ```java
   :#{#categories == null || #categories.isEmpty()} = true OR p.category IN :categories
   ```

3. **채팅 읽음 처리 설계**: 채팅방 입장 시점에 안읽은 메시지를 일괄 읽음 처리하는 방식이 효율적 (메시지 하나씩 처리하면 N+1 문제)

4. **AI API 보안**: 프론트에서 직접 외부 API를 호출하면 API 키가 노출됨. 백엔드를 프록시로 두어 키를 서버에서만 관리해야 안전

5. **React Fragment (`<>...</>`)**: 하나의 컴포넌트에서 모달처럼 최상위 요소 외부에 렌더링할 요소가 있을 때 Fragment로 감싸면 DOM에 불필요한 wrapper div가 생기지 않음
