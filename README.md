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
        │   ├── user/          # 회원 엔티티
        │   ├── product/       # 상품 엔티티 + 상태 Enum
        │   ├── chat/          # 채팅방 + 채팅 메시지 엔티티
        │   ├── wish/          # 찜 엔티티
        │   └── review/        # 리뷰 + 신고 엔티티
        └── global/
            ├── config/        # Security, WebSocket 설정 (예정)
            ├── jwt/           # JWT 필터, 유틸 (예정)
            └── exception/     # 공통 예외처리 (예정)
```

---

## 🗄 데이터베이스 ERD

총 7개 테이블로 구성됩니다.

| 테이블 | 설명 |
|--------|------|
| `users` | 회원 정보 (이메일, 비밀번호, 닉네임, 위치, 줍줍 점수) |
| `products` | 상품 정보 (제목, 설명, 가격, 카테고리, 거래 상태) |
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

## 📅 개발 일정

### ✅ 1주차 완료 (오늘)
- [x] Spring Boot 프로젝트 세팅 (Gradle + Java 21)
- [x] MySQL 연결 및 DB 생성
- [x] JWT 의존성 추가 (jjwt 0.12.6)
- [x] 패키지 구조 설계
- [x] 7개 엔티티 설계 및 테이블 자동 생성 확인
- [x] GitHub 레포지토리 연동

### 🔜 1주차 남은 작업
- [ ] 회원가입 API (`POST /api/auth/signup`)
- [ ] 로그인 API (`POST /api/auth/login`)
- [ ] JWT 토큰 발급 및 검증 (`JwtUtil`, `JwtFilter`)
- [ ] Spring Security 설정 (`SecurityConfig`)

### 📋 2주차
- [ ] 상품 CRUD API
- [ ] 카테고리 / 가격 필터 검색
- [ ] 이미지 업로드
- [ ] 거래 상태 변경 API

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

spring.jpa.hibernate.ddl-auto=create
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

jwt.secret=jupjup-secret-key-please-change-this-in-production
jwt.expiration=86400000
```

### 서버 실행
```bash
./gradlew bootRun
```

서버가 정상 실행되면 `http://localhost:8080` 으로 접근 가능합니다.
