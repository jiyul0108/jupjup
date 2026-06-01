# 📘 2주차 개발 기록

> **작업 기간**: 2주차  
> **작업 내용**: 상품 CRUD · 카테고리/가격 검색 · 이미지 업로드 · 거래 상태 관리

---

## 1. DTO 패키지 구조 정리

기존 1주차에서 `domain/user/` 바로 아래 있던 DTO 파일들을 `dto/` 폴더로 이동하여 구조를 정리했습니다.

```
domain/user/
├── User.java
├── UserRepository.java
├── UserService.java
├── AuthController.java
└── dto/
    ├── SignupRequest.java    ← 이동
    ├── LoginRequest.java     ← 이동
    └── AuthResponse.java     ← 이동

domain/product/
├── Product.java
├── ProductStatus.java
├── ProductRepository.java
├── ProductService.java
├── ProductController.java
├── ProductImage.java         ← 신규 추가
├── ProductImageRepository.java ← 신규 추가
├── ImageService.java         ← 신규 추가
└── dto/
    ├── ProductCreateRequest.java ← 신규 추가
    ├── ProductUpdateRequest.java ← 신규 추가
    └── ProductResponse.java      ← 신규 추가
```

**주요 포인트**
- IntelliJ **리팩터링 → 클래스 이동** 기능으로 이동 시 import 경로가 자동으로 수정됨
- 도메인별로 dto 폴더를 분리하면 파일이 많아질수록 관리가 쉬워짐

---

## 2. 상품 CRUD 구현

### ProductRepository

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> search(
            @Param("category") String category,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);
}
```

**주요 포인트**
- `LEFT JOIN FETCH p.images` — 상품 조회 시 이미지 목록을 한 번에 가져옴 (N+1 문제 방지)
- `(:category IS NULL OR ...)` — 파라미터가 null이면 해당 조건을 무시 (동적 쿼리)
- `findByIdWithImages` — 상세 조회 시 이미지 포함해서 조회하는 전용 메서드

---

### DTO

**ProductCreateRequest.java**

```java
@Getter
@NoArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotBlank(message = "카테고리는 필수입니다.")
    private String category;

    private String location;
}
```

**ProductUpdateRequest.java**

```java
@Getter
@NoArgsConstructor
public class ProductUpdateRequest {
    private String title;
    private String description;
    private int price;
    private String category;
    private String location;
}
```

**ProductResponse.java**

```java
@Getter
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String title;
    private int price;
    private String category;
    private String location;
    private String status;
    private int viewCount;
    private String sellerNickname;
    private LocalDateTime createdAt;
    private List<String> imageUrls;

    public static ProductResponse from(Product product) {
        List<String> imageUrls = product.getImages().stream()
                .map(image -> image.getImageUrl())
                .collect(Collectors.toList());

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
}
```

**주요 포인트**
- `@NotBlank` — 빈 문자열, null, 공백만 있는 문자열 모두 거부
- `@NotNull` + `@Min` — 가격은 null이 아니고 0원 이상이어야 함
- `from()` 정적 팩토리 메서드 — 엔티티를 DTO로 변환하는 로직을 DTO 안에 캡슐화

---

### ProductService

```java
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
```

**주요 포인트**
- `@Transactional` — 상품 등록 시 상품 저장 + 이미지 저장이 하나의 트랜잭션으로 처리됨
- 수정/삭제/상태변경 시 `product.getSeller().getEmail().equals(email)` 로 본인 확인
- `@AuthenticationPrincipal` 로 받은 이메일을 서비스 계층까지 전달하는 방식

---

### ProductController

```java
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
```

**주요 포인트**
- `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` — 상품 등록 시 multipart/form-data 형식으로 요청 수신
- `@RequestPart("data")` — multipart 요청에서 JSON 데이터 파트 추출
- `@RequestPart(value = "images", required = false)` — 이미지는 선택 항목
- `@PageableDefault` — 기본 페이지 크기 20, 최신순 정렬
- `@PatchMapping` — 리소스 일부(상태)만 변경할 때 사용하는 HTTP 메서드

---

## 3. 이미지 업로드 구현

### ProductImage 엔티티

```java
@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String imageUrl;
}
```

**주요 포인트**
- 상품 1개에 이미지 N개를 저장할 수 있도록 별도 테이블로 분리
- `@ManyToOne` — 여러 이미지가 하나의 상품에 속하는 다대일 관계

---

### Product 엔티티 수정 사항

```java
// images 필드 추가
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
@Builder.Default
private List<ProductImage> images = new ArrayList<>();

// update 메서드 추가
public void update(String title, String description, int price, String category, String location) {
    this.title = title;
    this.description = description;
    this.price = price;
    this.category = category;
    this.location = location;
}

// updateStatus 메서드 추가
public void updateStatus(ProductStatus status) {
    this.status = status;
}
```

**주요 포인트**
- `cascade = CascadeType.ALL` — 상품 삭제 시 이미지도 함께 삭제
- `orphanRemoval = true` — 상품과 연결이 끊긴 이미지는 자동 삭제
- `fetch = FetchType.EAGER` — 상품 조회 시 이미지 목록을 함께 로딩

---

### ImageService

```java
@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ProductImageRepository productImageRepository;

    public List<String> uploadImages(Product product, List<MultipartFile> files) throws IOException {
        // 절대 경로로 업로드 폴더 생성
        String absoluteUploadDir = System.getProperty("user.dir") + "/" + uploadDir;
        File dir = new File(absoluteUploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            // 파일명 중복 방지를 위해 UUID 사용
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = new File(absoluteUploadDir + "/" + fileName);
            file.transferTo(dest);

            String imageUrl = "/uploads/" + fileName;
            imageUrls.add(imageUrl);

            // DB에 저장
            ProductImage productImage = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .build();
            productImageRepository.save(productImage);
        }

        return imageUrls;
    }
}
```

**주요 포인트**
- `System.getProperty("user.dir")` — 프로젝트 루트 기준 절대 경로를 사용해야 Tomcat 임시 폴더 문제가 없음
- `UUID.randomUUID()` — 파일명 앞에 UUID를 붙여 중복 방지
- `dir.mkdirs()` — uploads 폴더가 없으면 자동 생성
- 업로드된 이미지는 `/uploads/{파일명}` URL로 접근 가능

---

### WebConfig — 정적 리소스 경로 설정

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/" + uploadDir + "/");
    }
}
```

**주요 포인트**
- `addResourceHandler("/uploads/**")` — `/uploads/`로 시작하는 URL 요청을 정적 리소스로 처리
- `addResourceLocations("file:...")` — 로컬 파일 시스템 경로를 URL과 매핑

---

### application.properties 추가 설정

```properties
# 이미지 업로드 경로
file.upload-dir=uploads
```

---

## 4. 유효성 검사 설정

### build.gradle 의존성 추가

```groovy
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

### SecurityConfig 수정 — /error 경로 허용

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/error").permitAll()        // 유효성 검사 실패 시 400 응답을 위해 추가
        .requestMatchers("/uploads/**").permitAll()   // 이미지 접근 허용
        .anyRequest().authenticated()
)
```

**주요 포인트**
- Spring Boot + Spring Security 환경에서 유효성 검사 실패 시 `/error`로 리다이렉트됨
- Spring Security가 `/error`를 기본적으로 차단하여 400 대신 403이 반환되는 문제 해결
- `/error` 경로를 `permitAll()`로 허용해야 올바른 400 에러 응답이 클라이언트에 전달됨

---

## 5. API 테스트 결과

Postman으로 테스트 완료 ✅

**상품 등록 (`POST /api/products`)**

```
form-data:
  data (Text, application/json): {"title":"아이폰 14","price":900000,"category":"디지털/가전","location":"서울"}
  images (File): 이미지 파일
```

```json
// 응답
{
    "id": 9,
    "title": "아이폰 14",
    "price": 900000,
    "category": "디지털/가전",
    "location": "서울",
    "status": "SELLING",
    "viewCount": 0,
    "sellerNickname": "줍줍유저",
    "createdAt": "2026-06-01T14:08:42.192075",
    "imageUrls": [
        "/uploads/3cc0b3d6-5c89-421b-807b-c09afc00f1df_하이미야.png"
    ]
}
```

**상품 목록 조회 — 카테고리 필터 (`GET /api/products?category=디지털/가전`)**

```json
// 응답 (content 배열 안에 상품 목록)
{
    "content": [...],
    "totalElements": 1,
    "totalPages": 1
}
```

**거래 상태 변경 (`PATCH /api/products/9/status?status=RESERVED`)**

```json
// 응답
{
    "id": 9,
    "status": "RESERVED",
    ...
}
```

---

## ✅ 2주차 완료 체크리스트

- [x] DTO 패키지 구조 정리 (`domain/user/dto`, `domain/product/dto`)
- [x] ProductRepository 구현 (동적 검색 쿼리, 이미지 포함 조회)
- [x] DTO 작성 (ProductCreateRequest, ProductUpdateRequest, ProductResponse)
- [x] 유효성 검사 추가 (`@Valid`, `@NotBlank`, `@NotNull`, `@Min`)
- [x] ProductService 구현 (CRUD + 이미지 업로드 + 상태 변경)
- [x] ProductController 구현 (6개 API 엔드포인트)
- [x] ProductImage 엔티티 추가
- [x] ImageService 구현 (로컬 파일 저장, UUID 파일명)
- [x] WebConfig 구현 (정적 리소스 경로 설정)
- [x] SecurityConfig 수정 (`/error`, `/uploads/**` 허용)
- [x] Postman 테스트 완료