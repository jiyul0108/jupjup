# 📘 1주차 개발 기록

> **작업 기간**: 1주차  
> **작업 내용**: 프로젝트 초기 세팅 · ERD 설계 · 회원가입/로그인 API · JWT 인증 구현

---

## 1. 프로젝트 초기 세팅

### 기술 스택 선택

| 항목 | 선택 | 이유 |
|------|------|------|
| Build | Gradle | Maven보다 간결한 문법, 빠른 빌드 |
| Java | 21 | 최신 LTS 버전 |
| Spring Boot | 4.0.6 | 최신 안정 버전 |
| Config | application.properties | 단순하고 직관적 |

---

### application.properties 설정

```properties
# 서버 포트
server.port=8080

# MySQL 연결
spring.datasource.url=jdbc:mysql://localhost:3306/jupjup?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=비밀번호
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 설정
spring.jpa.hibernate.ddl-auto=create
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT 설정
jwt.secret=jupjup-secret-key-please-change-this-in-production
jwt.expiration=86400000
```

**주요 설정 설명**

- `allowPublicKeyRetrieval=true` — MySQL 8.x에서 공개키 조회를 허용 (없으면 연결 오류 발생)
- `ddl-auto=create` — 서버 실행 시 엔티티 기반으로 테이블을 자동 생성 (개발 초기에만 사용, 이후 `update`로 변경 예정)
- `show-sql=true` — 실행되는 SQL을 콘솔에 출력 (디버깅용)
- `jwt.expiration=86400000` — 토큰 유효시간 86400000ms = 24시간

---

### JWT 의존성 추가 (build.gradle)

```groovy
// JWT
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

- `jjwt-api` — JWT 생성/파싱에 필요한 인터페이스 제공
- `jjwt-impl` — 실제 구현체 (런타임에만 필요)
- `jjwt-jackson` — JWT를 JSON으로 직렬화/역직렬화 (런타임에만 필요)

---

## 2. 패키지 구조 설계

```
com.jupjup.Backend
├── domain
│   ├── user        # 회원 관련 (엔티티, API, 서비스 등)
│   ├── product     # 상품 관련
│   ├── chat        # 채팅 관련
│   ├── wish        # 찜 관련
│   └── review      # 리뷰/신고 관련
└── global
    ├── config      # 전역 설정 (Security 등)
    ├── jwt         # JWT 관련 유틸, 필터
    └── exception   # 공통 예외처리 (예정)
```

- `domain` — 각 기능별로 엔티티, 서비스, 컨트롤러를 함께 묶는 **도메인 중심 패키지 구조**
- `global` — 특정 도메인에 속하지 않는 전역 설정/유틸 모음

---

## 3. ERD 설계 및 엔티티 구현

### 전체 테이블 구조

총 7개 테이블, 엔티티 작성 시 JPA가 자동으로 테이블을 생성해줍니다.

---

### User 엔티티

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    private String profileImage;
    private String location;

    @Column(nullable = false)
    private int jupjupScore = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

**주요 포인트**

- `@Table(name = "users")` — MySQL에서 `user`는 예약어라 충돌 방지를 위해 `users`로 지정
- `unique = true` — 이메일, 닉네임 중복 방지를 DB 레벨에서 보장
- `jupjupScore = 0` — 당근마켓의 매너온도(36.5°) 대신 **줍줍만의 점수 시스템**으로 차별화, 0점부터 시작
- `@PrePersist` — DB 저장 직전에 자동으로 `createdAt` 현재 시각 세팅
- `updatable = false` — 생성 시각은 한번 저장되면 수정 불가

---

### Product 엔티티 + ProductStatus Enum

```java
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.SELLING;

    private String location;

    @Builder.Default
    private int viewCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

```java
public enum ProductStatus {
    SELLING,    // 판매중
    RESERVED,   // 예약중
    SOLD        // 거래완료
}
```

**주요 포인트**

- `@ManyToOne(fetch = FetchType.LAZY)` — 상품과 판매자는 다대일 관계. `LAZY`로 설정해 상품 조회 시 판매자 정보를 즉시 불러오지 않아 성능 최적화
- `@JoinColumn(name = "seller_id")` — DB에 `seller_id` 외래키 컬럼 생성
- `@Enumerated(EnumType.STRING)` — DB에 숫자(0,1,2)가 아닌 문자열("SELLING")로 저장해 가독성 향상
- `columnDefinition = "TEXT"` — 상품 설명은 길 수 있어서 VARCHAR 대신 TEXT 타입 사용

---

### ChatRoom / ChatMessage 엔티티

```java
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
}
```

```java
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
```

**주요 포인트**

- `ChatRoom` — 상품 1개 + 구매자 1명 + 판매자 1명으로 채팅방을 구성. 한 상품에 여러 채팅방이 생길 수 있음
- `ChatMessage` — 채팅방에 속한 메시지. 누가 보냈는지(`sender_id`) 기록

---

### Wish / Review / Report 엔티티

```java
// 찜
@Entity
@Table(name = "wishes")
public class Wish {
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;
}

// 리뷰
@Entity
@Table(name = "reviews")
public class Review {
    @ManyToOne(fetch = FetchType.LAZY)
    private User reviewer;   // 리뷰 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    private User reviewee;   // 리뷰 대상자

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private int score;       // 줍줍 점수에 반영 예정
}

// 신고
@Entity
@Table(name = "reports")
public class Report {
    @ManyToOne(fetch = FetchType.LAZY)
    private User reporter;   // 신고자

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product; // 신고 대상 상품

    private String reason;   // 신고 사유
}
```

**주요 포인트**

- `Wish` — 유저와 상품을 연결하는 중간 테이블 역할. 한 유저가 여러 상품을 찜할 수 있음
- `Review` — `reviewer`(작성자)와 `reviewee`(대상자)를 분리해 양방향 리뷰 가능
- `Report` — 부적절한 상품 신고 기능. 신고 사유를 텍스트로 기록

---

## 4. JWT 인증 구현

### 전체 인증 흐름

```
클라이언트 요청
    ↓
JwtFilter — Authorization 헤더에서 토큰 추출 및 검증
    ↓
SecurityConfig — 경로별 인증 허용/차단 결정
    ↓
AuthController — 회원가입 / 로그인 엔드포인트
    ↓
UserService — 비즈니스 로직 처리
    ↓
UserRepository — DB 저장 / 조회
```

---

### JwtUtil — 토큰 생성 / 파싱 / 검증

```java
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    // 토큰 생성
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    // 토큰에서 이메일 추출
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

**주요 포인트**

- `@Value("${jwt.secret}")` — `application.properties`에서 secret key 값을 주입받음
- `Keys.hmacShaKeyFor` — 문자열 secret key를 HMAC-SHA 알고리즘용 키 객체로 변환
- `subject(email)` — 토큰 안에 이메일을 담아 나중에 꺼내 사용
- `validateToken` — 파싱 시 예외가 발생하면 유효하지 않은 토큰으로 판단

---

### JwtFilter — 요청마다 토큰 검사

```java
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmailFromToken(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, List.of());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

**주요 포인트**

- `OncePerRequestFilter` — 하나의 요청에 딱 한 번만 실행되도록 보장
- `resolveToken` — `Authorization: Bearer {토큰}` 헤더에서 `Bearer ` 이후의 토큰 값만 추출
- `SecurityContextHolder` — 인증 정보를 저장하는 Spring Security의 전역 저장소. 여기에 저장하면 이후 컨트롤러에서 인증된 사용자 정보 사용 가능

---

### SecurityConfig — 경로별 인증 설정

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**주요 포인트**

- `csrf disable` — REST API는 stateless 방식이라 CSRF 공격 위험이 없어 비활성화
- `SessionCreationPolicy.STATELESS` — JWT 방식이므로 서버에 세션을 만들지 않음
- `permitAll()` — `/api/auth/**` 경로는 토큰 없이 접근 허용 (회원가입, 로그인)
- `anyRequest().authenticated()` — 나머지 모든 요청은 토큰 필수
- `addFilterBefore` — Spring Security의 기본 필터보다 JwtFilter가 먼저 실행되도록 등록

---

## 5. 회원가입 / 로그인 API 구현

### UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);      // 로그인 시 이메일로 조회
    boolean existsByEmail(String email);            // 이메일 중복 체크
    boolean existsByNickname(String nickname);      // 닉네임 중복 체크
}
```

**주요 포인트**

- `JpaRepository<User, Long>` — 기본 CRUD (`save`, `findById`, `findAll` 등)를 자동 제공
- 메서드명 규칙(`findBy`, `existsBy`)만 지키면 Spring Data JPA가 쿼리를 자동 생성

---

### DTO (Data Transfer Object)

```java
// 회원가입 요청
public class SignupRequest {
    private String email;
    private String password;
    private String nickname;
    private String location;
}

// 로그인 요청
public class LoginRequest {
    private String email;
    private String password;
}

// 인증 응답
public class AuthResponse {
    private String token;    // JWT 토큰
    private String email;
    private String nickname;
}
```

**주요 포인트**

- DTO는 API 요청/응답에 필요한 데이터만 담는 전용 객체
- 엔티티를 직접 노출하지 않아 보안상 안전하고 필요한 데이터만 주고받을 수 있음

---

### UserService — 비즈니스 로직

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원가입
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .location(request.getLocation())
                .build();

        userRepository.save(user);
    }

    // 로그인
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getNickname());
    }
}
```

**주요 포인트**

- `passwordEncoder.encode` — 비밀번호를 BCrypt로 암호화해서 저장. 절대 평문으로 저장하면 안 됨
- `passwordEncoder.matches` — 입력한 비밀번호와 DB의 암호화된 비밀번호를 비교. BCrypt는 같은 값도 매번 다르게 암호화되기 때문에 `==` 비교 불가
- `orElseThrow` — 이메일로 유저를 못 찾으면 바로 예외 발생
- `@RequiredArgsConstructor` — `final` 필드를 생성자 주입으로 자동 처리 (Lombok)

---

### AuthController — API 엔드포인트

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

**주요 포인트**

- `@RestController` — `@Controller` + `@ResponseBody`. 모든 메서드가 JSON으로 응답
- `@RequestMapping("/api/auth")` — 이 컨트롤러의 모든 경로 앞에 `/api/auth`가 붙음
- `@RequestBody` — HTTP 요청의 Body(JSON)를 Java 객체로 자동 변환
- `ResponseEntity.ok()` — HTTP 200 상태코드와 함께 응답 반환

---

## 6. API 테스트 결과

Postman으로 테스트 완료 ✅

**회원가입 (`POST /api/auth/signup`)**
```json
// 요청
{
  "email": "test@test.com",
  "password": "1234",
  "nickname": "줍줍유저",
  "location": "서울"
}

// 응답
"회원가입이 완료되었습니다."
```

**로그인 (`POST /api/auth/login`)**
```json
// 요청
{
  "email": "test@test.com",
  "password": "1234"
}

// 응답
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwia...",
  "email": "test@test.com",
  "nickname": "줍줍유저"
}
```

---

## ✅ 1주차 완료 체크리스트

- [x] Spring Boot 프로젝트 세팅 (Gradle + Java 21)
- [x] MySQL 연결 및 jupjup DB 생성
- [x] JWT 의존성 추가
- [x] 패키지 구조 설계
- [x] 7개 엔티티 작성 및 테이블 자동 생성 확인
- [x] GitHub 레포지토리 연동
- [x] JwtUtil 구현 (토큰 생성 / 파싱 / 검증)
- [x] JwtFilter 구현 (요청마다 토큰 검사)
- [x] SecurityConfig 구현 (경로별 인증 설정)
- [x] UserRepository 구현
- [x] DTO 작성 (SignupRequest, LoginRequest, AuthResponse)
- [x] UserService 구현 (회원가입 / 로그인 로직)
- [x] AuthController 구현 (API 엔드포인트)
- [x] Postman 테스트 완료
