# 줍줍 (JupJup) 🛍️

> **내 동네 득템 마켓** — 당근마켓을 모티브로 한 중고거래 플랫폼

---

## 📌 프로젝트 소개

**줍줍**은 "줍다"에서 따온 이름으로, 내 동네에서 득템하는 재미를 담은 중고거래 플랫폼입니다.  
사용자는 판매자와 구매자 역할을 동시에 수행할 수 있으며, 상품 등록/검색부터 실시간 채팅을 통한 거래까지 중고거래의 전체 흐름을 구현합니다.

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Spring Boot 4.0.6 |
| Language | Java 21 |
| Build | Gradle |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Database | MySQL 8.x + Spring Data JPA (Hibernate 7) |
| 실시간 채팅 | WebSocket (STOMP) |
| Frontend | React 18 (예정) |

---

## 📁 프로젝트 구조

```
JupJup/
├── frontend/          # React 프로젝트 (예정)
└── backend/           # Spring Boot 프로젝트
    └── src/main/java/com/jupjup/Backend/
        ├── domain/
        │   ├── user/          # 회원 엔티티 + 회원가입/로그인 API
        │   ├── product/       # 상품 엔티티 + CRUD + 이미지 업로드 + 상태 관리
        │   ├── chat/          # 채팅방 + 채팅 메시지 엔티티
        │   ├── wish/          # 찜 엔티티
        │   └── review/        # 리뷰 + 신고 엔티티
        └── global/
            ├── config/        # Security 설정, WebMvc 설정
            ├── jwt/           # JWT 필터, 유틸
            └── exception/     # 공통 예외처리 (예정)
```

---

## 🗄 데이터베이스 ERD

총 8개 테이블로 구성됩니다.

| 테이블 | 설명 |
|--------|------|
| `users` | 회원 정보 (이메일, 비밀번호, 닉네임, 위치, 줍줍 점수) |
| `products` | 상품 정보 (제목, 설명, 가격, 카테고리, 거래 상태) |
| `product_images` | 상품 이미지 (상품 1개 : 이미지 N개) |
| `chat_rooms` | 채팅방 (상품, 구매자, 판매자 연결) |
| `chat_messages` | 채팅 메시지 (채팅방, 발신자, 내용) |
| `wishes` | 찜 목록 (회원 ↔ 상품) |
| `reviews` | 리뷰 (작성자, 대상자, 상품, 점수) |
| `reports` | 신고 (신고자, 상품, 사유) |

---

## 💡 주요 코드 특징

### 1. 엔티티 설계 — JPA + Lombok 조합

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    ...
}
```

- `@Entity` — 이 클래스가 DB 테이블과 연결되는 JPA 엔티티임을 선언
- `@Table(name = "users")` — MySQL에서 `user`는 예약어이므로 테이블명을 `users`로 지정
- `@Getter` / `@Builder` — Lombok이 getter와 빌더 패턴을 자동 생성 (보일러플레이트 제거)
- `@NoArgsConstructor` — JPA는 기본 생성자가 필수이므로 반드시 필요

---

### 2. 줍줍 점수 — 당근마켓의 매너온도를 차별화

```java
@Column(nullable = false)
private int jupjupScore = 0;
```

- 당근마켓의 "매너온도(36.5°)"를 그대로 따라가지 않고 **줍줍 점수(0점 시작)** 로 차별화
- 거래가 쌓일수록 점수가 올라가는 방식으로 설계 예정

---

### 3. 거래 상태 — Enum 타입으로 관리

```java
public enum ProductStatus {
    SELLING,    // 판매중
    RESERVED,   // 예약중
    SOLD        // 거래완료
}
```

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private ProductStatus status = ProductStatus.SELLING;
```

- `@Enumerated(EnumType.STRING)` — DB에 숫자(0,1,2)가 아닌 문자열("SELLING")로 저장하여 가독성 향상
- 상품 등록 시 기본값은 `SELLING`(판매중)

---

### 4. 연관관계 매핑 — @ManyToOne + FetchType.LAZY

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "seller_id", nullable = false)
private User seller;
```

- `@ManyToOne` — 여러 상품이 한 명의 판매자(User)에 속하는 다대일 관계
- `FetchType.LAZY` — 상품 조회 시 판매자 정보를 즉시 불러오지 않고 필요할 때만 조회 (성능 최적화)
- `@JoinColumn(name = "seller_id")` — DB에 `seller_id` 외래키 컬럼 생성

---

### 5. @PrePersist — 생성 시각 자동 저장

```java
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;

@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
}
```

- `@PrePersist` — DB에 INSERT되기 직전에 자동으로 실행되는 메서드
- `updatable = false` — 최초 저장 후 수정 불가능하도록 설정

---

### 6. JWT 인증 — JwtUtil

```java
public String generateToken(String email) {
    return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey)
            .compact();
}
```

- 로그인 성공 시 이메일을 담아 JWT 토큰을 생성
- 토큰 유효시간은 `application.properties`의 `jwt.expiration`으로 관리 (기본 24시간)
- `Keys.hmacShaKeyFor` — secret key를 HMAC-SHA 알고리즘으로 변환하여 서명에 사용

---

### 7. JWT 필터 — JwtFilter

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    String token = resolveToken(request);

    if (token != null && jwtUtil.validateToken(token)) {
        String email = jwtUtil.getEmailFromToken(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
}
```

- 모든 요청마다 `Authorization: Bearer {토큰}` 헤더를 검사
- 토큰이 유효하면 `SecurityContextHolder`에 인증 정보 저장 → Spring Security가 인증된 사용자로 인식

---

### 8. Security 설정 — SecurityConfig

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/uploads/**").permitAll()
            .anyRequest().authenticated()
    )
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

- `csrf disable` — REST API는 CSRF 보호가 불필요하여 비활성화
- `STATELESS` — JWT 방식이므로 서버에 세션을 저장하지 않음
- `/api/auth/**` — 회원가입, 로그인은 토큰 없이 접근 허용, 나머지는 토큰 필수
- `/uploads/**` — 업로드된 이미지 파일 접근 허용

---

### 9. 비밀번호 암호화 — BCrypt

```java
// 회원가입 시 암호화
password = passwordEncoder.encode(request.getPassword())

// 로그인 시 검증
passwordEncoder.matches(request.getPassword(), user.getPassword())
```

- 비밀번호를 BCrypt 알고리즘으로 암호화하여 DB에 저장 (평문 저장 금지)
- `matches` — 입력한 비밀번호와 암호화된 비밀번호를 안전하게 비교

---

### 10. 상품 검색 필터 — @Query 동적 쿼리

```java
@Query(value = "SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE " +
        "(:category IS NULL OR p.category = :category) AND " +
        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
        "(:maxPrice IS NULL OR p.price <= :maxPrice)",
        countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
        "(:category IS NULL OR p.category = :category) AND " +
        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
        "(:maxPrice IS NULL OR p.price <= :maxPrice)")
Page<Product> search(
        @Param("category") String category,
        @Param("minPrice") Integer minPrice,
        @Param("maxPrice") Integer maxPrice,
        Pageable pageable
);
```

- 카테고리, 최소 가격, 최대 가격을 선택적으로 필터링
- 파라미터가 `null`이면 해당 조건을 무시 → 전체 조회와 동일하게 동작
- `LEFT JOIN FETCH p.images` — 이미지 목록을 한 번에 조회 (N+1 문제 방지)
- `countQuery` 분리 — `JOIN FETCH`와 `Page`를 함께 쓸 때 전체 데이터를 메모리에 올리는 문제 방지
- `Pageable` — 페이지 번호, 페이지 크기, 정렬 기준을 쿼리 파라미터로 전달 가능

---

### 11. 이미지 업로드 — MultipartFile + 로컬 저장

```java
String absoluteUploadDir = System.getProperty("user.dir") + "/" + uploadDir;
File dir = new File(absoluteUploadDir);
if (!dir.exists()) {
    dir.mkdirs();
}

String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
File dest = new File(absoluteUploadDir + "/" + fileName);
file.transferTo(dest);
```

- `MultipartFile` — 이미지 파일을 multipart/form-data 형식으로 수신
- `UUID` — 파일명 중복 방지를 위해 랜덤 UUID를 파일명 앞에 붙임
- `System.getProperty("user.dir")` — 프로젝트 루트 기준 절대 경로로 저장
- 저장된 이미지는 `/uploads/{파일명}` URL로 접근 가능

---

### 12. 거래 상태 변경 — PATCH API

```java
@PatchMapping("/{id}/status")
public ResponseEntity<ProductResponse> updateStatus(
        @PathVariable Long id,
        @RequestParam ProductStatus status,
        @AuthenticationPrincipal String email) {
    return ResponseEntity.ok(productService.updateStatus(id, status, email));
}
```

- `PATCH` — 리소스의 일부(상태)만 변경하는 HTTP 메서드
- 본인 상품만 상태 변경 가능 (다른 사람 상품 변경 시 예외 발생)
- `SELLING` → `RESERVED` → `SOLD` 순으로 상태 전환

---

## 🔌 API 명세

### 인증 API

| Method | URL | 설명 | 인증 필요 |
|--------|-----|------|----------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 | ❌ |

**회원가입 요청**
```json
{
  "email": "test@test.com",
  "password": "1234",
  "nickname": "줍줍유저",
  "location": "서울"
}
```

**로그인 응답**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "email": "test@test.com",
  "nickname": "줍줍유저"
}
```

---

### 상품 API

| Method | URL | 설명 | 인증 필요 |
|--------|-----|------|----------|
| POST | `/api/products` | 상품 등록 (이미지 포함) | ✅ |
| GET | `/api/products` | 상품 목록 조회 (필터 검색) | ✅ |
| GET | `/api/products/{id}` | 상품 상세 조회 | ✅ |
| PUT | `/api/products/{id}` | 상품 수정 | ✅ |
| DELETE | `/api/products/{id}` | 상품 삭제 | ✅ |
| PATCH | `/api/products/{id}/status` | 거래 상태 변경 | ✅ |

**상품 등록 요청 (multipart/form-data)**

| Key | Type | 설명 |
|-----|------|------|
| data | Text (application/json) | `{"title":"아이폰 14","price":900000,"category":"디지털/가전","location":"서울"}` |
| images | File | 이미지 파일 (선택) |

**상품 목록 조회 쿼리 파라미터**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| category | String | 카테고리 필터 (선택) |
| minPrice | Integer | 최소 가격 (선택) |
| maxPrice | Integer | 최대 가격 (선택) |
| sort | String | 정렬 기준 (기본: 최신순) |

**상품 조회 응답**
```json
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

**거래 상태 변경 요청**
```
PATCH /api/products/9/status?status=RESERVED
```

---

## 📅 개발 일정

### ✅ 1주차 완료
- [x] Spring Boot 프로젝트 세팅 (Gradle + Java 21)
- [x] MySQL 연결 및 DB 생성
- [x] JWT 의존성 추가 (jjwt 0.12.6)
- [x] 패키지 구조 설계
- [x] 7개 엔티티 설계 및 테이블 자동 생성 확인
- [x] GitHub 레포지토리 연동
- [x] 회원가입 API (`POST /api/auth/signup`)
- [x] 로그인 API (`POST /api/auth/login`)
- [x] JWT 토큰 발급 및 검증 (`JwtUtil`, `JwtFilter`)
- [x] Spring Security 설정 (`SecurityConfig`)

### ✅ 2주차 완료
- [x] 상품 CRUD API (등록/조회/수정/삭제)
- [x] 유효성 검사 (`@Valid` — 필수 필드 누락 시 400 에러)
- [x] 카테고리 / 가격 필터 검색 (`@Query` 동적 쿼리)
- [x] 페이지네이션 (`Pageable` — 기본 20개, 최신순 정렬)
- [x] 이미지 업로드 (`MultipartFile` — 로컬 저장, UUID 파일명)
- [x] 거래 상태 변경 API (`PATCH /api/products/{id}/status`)
- [x] DTO 패키지 구조 정리 (`domain/user/dto`, `domain/product/dto`)

### 📋 3주차
- [ ] WebSocket 채팅 구현
- [ ] 찜하기 기능
- [ ] 리뷰 작성 및 줍줍 점수 반영
- [ ] 신고 기능

### 📋 4주차
- [ ] React 프론트엔드 연동
- [ ] 예외 처리 고도화
- [ ] 테스트 및 디버깅
- [ ] README 최종 정리

---

## ⚙️ 실행 방법

### 사전 준비
- Java 21
- MySQL 8.x
- `jupjup` 데이터베이스 생성

```sql
CREATE DATABASE jupjup DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### application.properties 설정

```properties
spring.application.name=jupjup
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/jupjup?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=비밀번호입력
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

jwt.secret=jupjup-secret-key-please-change-this-in-production-very-long-key
jwt.expiration=86400000

# 이미지 업로드
file.upload-dir=uploads
```

### 서버 실행
```bash
./gradlew bootRun
```

서버가 정상 실행되면 `http://localhost:8080` 으로 접근 가능합니다.