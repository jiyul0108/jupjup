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
| 실시간 채팅 | WebSocket (STOMP + SockJS) |
| Frontend | React 18 + Vite + Tailwind CSS |
| AI 기능 | Google Gemini API (gemini-2.5-flash) |

---

## 📁 프로젝트 구조

```
JupJup/
├── frontend/                    # React + Vite 프로젝트
│   └── src/
│       ├── pages/               # 페이지 컴포넌트
│       │   ├── MainPage.jsx         # 메인 상품 목록 + 검색 + 필터
│       │   ├── ProductDetailPage.jsx # 상품 상세 + 이미지 슬라이더
│       │   ├── ProductFormPage.jsx   # 상품 등록/수정 + AI 설명 작성
│       │   ├── ChatPage.jsx          # 채팅방 목록 + 실시간 채팅
│       │   ├── WishListPage.jsx      # 찜 목록
│       │   ├── PurchasePage.jsx      # 구매 페이지
│       │   ├── OrderDetailPage.jsx   # 주문 상세
│       │   └── MyPage.jsx            # 마이페이지
│       ├── components/
│       │   ├── Header.jsx            # 헤더 (검색 + 채팅 뱃지)
│       │   └── ProductCard.jsx       # 상품 카드
│       └── api/index.js             # axios API 함수 모음
│
└── Backend/                     # Spring Boot 프로젝트
    └── src/main/java/com/jupjup/Backend/
        ├── domain/
        │   ├── user/            # 회원 + 프로필 API
        │   ├── product/         # 상품 CRUD + 검색 + 이미지
        │   ├── chat/            # 채팅방 + 실시간 채팅 + 읽음 처리
        │   ├── wish/            # 찜하기
        │   ├── review/          # 리뷰 + 신고
        │   ├── order/           # 주문
        │   └── ai/              # AI 설명 생성 (Gemini API)
        └── global/
            ├── config/          # Security, WebMvc, WebSocket 설정
            ├── jwt/             # JWT 필터, 유틸, 핸드셰이크 인터셉터
            └── exception/       # 공통 예외처리
```

---

## 🗄 데이터베이스 ERD

총 9개 테이블로 구성됩니다.

| 테이블 | 설명 |
|--------|------|
| `users` | 회원 정보 (이메일, 비밀번호, 닉네임, 위치, 줍줍 점수) |
| `products` | 상품 정보 (제목, 설명, 가격, 카테고리, 거래 상태) |
| `product_images` | 상품 이미지 (상품 1개 : 이미지 N개) |
| `chat_rooms` | 채팅방 (상품, 구매자, 판매자 연결) |
| `chat_messages` | 채팅 메시지 (채팅방, 발신자, 내용, 읽음 여부) |
| `wishes` | 찜 목록 (회원 ↔ 상품) |
| `reviews` | 리뷰 (작성자, 대상자, 상품, 점수) |
| `reports` | 신고 (신고자, 상품, 사유) |
| `orders` | 주문 (구매자, 상품, 배송지, 결제방법) |

---

## ✨ 주요 기능

### 1. 회원 기능
- 회원가입 / 로그인 (JWT 토큰 기반 인증)
- 마이페이지 (프로필 조회, 내 판매/구매/찜/리뷰 확인)
- 개인정보 수정 (닉네임, 동네, 비밀번호 변경)
- **줍줍Score** — 당근마켓 매너온도 대신 점수 기반 평판 시스템 (씨앗🌱 → 새싹🌿 → 나무🌳)

### 2. 상품 기능
- 상품 등록 / 수정 / 삭제 (이미지 최대 5장)
- 거래 상태 관리 (판매중 / 예약중 / 거래완료)
- **AI 상품 설명 자동 작성** — Gemini AI가 제목 + 카테고리 + 메모를 기반으로 설명 자동 생성

### 3. 검색 & 필터
- 키워드 검색 (상품 제목)
- 검색 후 필터 버튼 표시 (가격 범위 / 카테고리 복수 선택 / 거래 상태)
- 정렬 (최신순 / 낮은 가격순 / 높은 가격순)

### 4. 채팅 기능
- WebSocket(STOMP + SockJS) 기반 실시간 1:1 채팅
- **읽음 처리** — 채팅방 입장 시 안읽은 메시지 자동 읽음 처리
- **안읽은 메시지 뱃지** — 헤더 채팅 버튼 및 채팅 목록에 숫자 뱃지 표시
- 내 메시지 옆 읽음 / 안읽음(1) 표시

### 5. 구매 & 주문
- 구매 페이지 (구매자 이름, 연락처, 배송지 주소, 결제 방법, 요청사항)
- 주문 완료 페이지 (주문 상세 확인)
- 주문 시 상품 상태 자동 거래완료 처리

### 6. 기타
- 상품 찜하기 (토글 방식)
- 거래 완료 후 리뷰 작성 (줍줍Score 자동 반영)
- 부적절한 상품 신고

---

## 🔌 API 명세

### 인증 API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 | ❌ |
| GET | `/api/auth/me` | 내 프로필 조회 | ✅ |
| PUT | `/api/auth/me` | 프로필 수정 | ✅ |
| GET | `/api/auth/me/products` | 내 판매 상품 목록 | ✅ |

### 상품 API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/products` | 상품 등록 (이미지 포함) | ✅ |
| GET | `/api/products` | 상품 목록 조회 (검색/필터) | ❌ |
| GET | `/api/products/{id}` | 상품 상세 조회 | ❌ |
| PUT | `/api/products/{id}` | 상품 수정 | ✅ |
| DELETE | `/api/products/{id}` | 상품 삭제 | ✅ |
| PATCH | `/api/products/{id}/status` | 거래 상태 변경 | ✅ |

**검색 파라미터**

| 파라미터 | 설명 | 예시 |
|---------|------|------|
| `keyword` | 제목 검색 | `?keyword=아이폰` |
| `categories` | 카테고리 (복수) | `?categories=의류&categories=식품` |
| `minPrice` | 최소 가격 | `?minPrice=10000` |
| `maxPrice` | 최대 가격 | `?maxPrice=50000` |
| `status` | 거래 상태 | `?status=SELLING` |
| `sort` | 정렬 | `?sort=price,asc` |

### 채팅 API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/chat/rooms` | 채팅방 생성 | ✅ |
| GET | `/api/chat/rooms` | 내 채팅방 목록 | ✅ |
| GET | `/api/chat/rooms/{roomId}/messages` | 이전 메시지 조회 | ✅ |
| GET | `/api/chat/unread` | 전체 안읽은 메시지 수 | ✅ |
| STOMP | `/pub/chat/{roomId}` | 실시간 메시지 전송 | ✅ |

### 주문 API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/orders` | 주문 생성 | ✅ |
| GET | `/api/orders` | 내 주문 목록 | ✅ |
| GET | `/api/orders/{id}` | 주문 상세 | ✅ |

### AI API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/ai/description` | AI 상품 설명 생성 | ✅ |

**AI 요청**
```json
{
  "title": "아이폰 14 프로",
  "category": "디지털/가전",
  "memo": "거의 새 제품, 충전기 포함"
}
```

### 찜 API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/wishes/{productId}` | 찜 추가/취소 | ✅ |
| GET | `/api/wishes` | 내 찜 목록 | ✅ |

### 리뷰 / 신고 API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/reviews` | 리뷰 작성 | ✅ |
| GET | `/api/reviews/users/{userId}` | 받은 리뷰 목록 | ✅ |
| POST | `/api/reports` | 상품 신고 | ✅ |

---

## 📅 개발 일정

### ✅ 1주차
- Spring Boot 프로젝트 세팅 (Gradle + Java 21)
- MySQL 연결 및 DB 생성
- 7개 엔티티 설계 및 테이블 자동 생성
- 회원가입 / 로그인 API
- JWT 토큰 발급 및 검증
- Spring Security 설정

### ✅ 2주차
- 상품 CRUD API (등록/조회/수정/삭제)
- 카테고리 / 가격 필터 검색
- 페이지네이션
- 이미지 업로드 (로컬 저장, UUID 파일명)
- 거래 상태 변경 API
- DTO 패키지 구조 정리

### ✅ 3주차
- WebSocket 실시간 채팅 (STOMP + SockJS)
- 채팅방 생성/조회 API
- 찜하기 토글 API
- 리뷰 작성 + 줍줍Score 자동 반영
- 신고 API
- 전역 예외처리 (`GlobalExceptionHandler`)

### ✅ 4주차
- React 프론트엔드 구현 (Vite + Tailwind CSS)
- 프론트-백엔드 연동 (LazyInitializationException 다수 수정)
- 상품 등록/수정/삭제/상태변경 UI
- 채팅 UI (SockJS 연동)
- 찜 목록 UI
- README 작성

### ✅ 5주차
- **검색 기능** — 키워드 검색 + 카테고리 복수 선택 + 가격/상태 필터
- **더보기 버튼** — 상품 목록 8개씩 페이지네이션, 더보기 클릭 시 추가 로드
- **이미지 슬라이더** — 썸네일 클릭으로 이미지 전환 + 확대 모달
- **줍줍Score 표시** — 상품 상세에서 판매자 점수 표시
- **구매/주문 기능** — 구매 페이지 + 주문 완료 페이지
- **채팅 읽음 처리** — 읽음/안읽음 상태, 뱃지 표시
- **채팅 UI 개선** — 채팅 목록/채팅방 가운데 박스 스타일로 변경
- **마이페이지** — 프로필, 내 판매/구매/찜/리뷰, 개인정보 수정
- **리뷰 작성 UI** — 마이페이지 내 구매 탭에서 별점 + 내용 작성 모달
- **신고 UI** — 상품 상세 페이지에서 신고 모달
- **AI 상품 설명 자동 작성** — Gemini API 백엔드 경유 호출, 모달 UI
- **브라우저 탭 설정** — 타이틀 `줍줍 - 중고거래`, 파비콘 적용
- **보안 강화** — application.properties .gitignore 처리
- **상품 삭제 버그 수정** — 연관 데이터(채팅/주문/찜/리뷰/신고) 순서대로 삭제

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

`application.properties.example`을 복사해서 `application.properties`로 만들고 값을 채워주세요.

```properties
spring.datasource.password=YOUR_DB_PASSWORD
jwt.secret=YOUR_JWT_SECRET_KEY
gemini.api.key=YOUR_GEMINI_API_KEY
```

### 백엔드 실행
```bash
cd Backend
./gradlew bootRun
```

### 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```

프론트: `http://localhost:5173`  
백엔드: `http://localhost:8080`

---

## 🧪 테스트 계정

| 역할 | 이메일 | 비밀번호 |
|------|--------|----------|
| 판매자 | test@test.com | 1234 |
| 구매자 | buyer@test.com | 1234 |
