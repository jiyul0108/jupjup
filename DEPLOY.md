# 줍줍 (JupJup) 배포 가이드

> **배포 구성**: 백엔드 → Railway / 프론트엔드 → Vercel / 이미지 → Cloudinary / DB → Railway MySQL

---

## 📋 배포 환경 정보

| 항목 | 서비스 | URL |
|------|--------|-----|
| 백엔드 | Railway | https://web-production-fd4e2.up.railway.app |
| 프론트엔드 | Vercel | https://jupjup-frontend.vercel.app |
| 데이터베이스 | Railway MySQL | mysql.railway.internal:3306 |
| 이미지 스토리지 | Cloudinary | cloud: dyq9hvnmg |

---

## 1. 백엔드 배포 (Railway)

### 1-1. 사전 준비

#### `build.gradle` — plain jar 비활성화
```gradle
// plain jar 생성 비활성화 (Railway 배포용)
jar {
    enabled = false
}
```

#### `Procfile` 생성
```
web: java -jar build/libs/Backend-0.0.1-SNAPSHOT.jar
```

#### `application.properties` — 환경변수 방식으로 수정
```properties
server.port=${PORT:8080}
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/jupjup...}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:비밀번호}
jwt.secret=${JWT_SECRET:시크릿키}
gemini.api.key=${GEMINI_API_KEY:}
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
cloudinary.api-key=${CLOUDINARY_API_KEY:}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

### 1-2. Railway 프로젝트 생성
1. `railway.app` 접속 → GitHub 로그인
2. **New Project → 깃허브 저장소** 선택
3. JupJup 레포 선택
4. **Settings → Source → Add Root Directory** → `Backend` 입력

### 1-3. MySQL 데이터베이스 추가
1. 프로젝트 대시보드 → **+ Add → Database → MySQL**
2. 생성된 MySQL 서비스 클릭 → **Variables** 탭에서 아래 값 확인:
   - `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`

### 1-4. 백엔드 환경변수 설정
**web 서비스 → Variables 탭**에서 추가:

```
SPRING_DATASOURCE_URL=jdbc:mysql://mysql.railway.internal:3306/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD={MYSQLPASSWORD 값}
JWT_SECRET=jupjup-secret-key-2024-very-long-key
GEMINI_API_KEY={Gemini API 키}
FILE_UPLOAD_DIR=uploads
CLOUDINARY_CLOUD_NAME={Cloudinary cloud name}
CLOUDINARY_API_KEY={Cloudinary API key}
CLOUDINARY_API_SECRET={Cloudinary API secret}
```

### 1-5. CORS 설정 (SecurityConfig.java)
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

> ⚠️ `WebConfig.java`의 `addCorsMappings`와 `SecurityConfig.java`의 CORS 설정 **둘 다** 있어야 정상 동작함

---

## 2. 이미지 스토리지 (Cloudinary)

### 2-1. Cloudinary 가입 및 설정
1. `cloudinary.com` 접속 → 회원가입
2. 대시보드에서 **Cloud Name, API Key, API Secret** 확인

### 2-2. 백엔드 의존성 추가 (`build.gradle`)
```gradle
implementation 'com.cloudinary:cloudinary-http44:1.39.0'
```

### 2-3. CloudinaryConfig.java 생성
```java
@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
    }
}
```

### 2-4. ImageService.java — Cloudinary 업로드로 변경
```java
Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
    ObjectUtils.asMap("folder", "jupjup", "resource_type", "image"));
String imageUrl = (String) uploadResult.get("secure_url");
```

### 2-5. 기존 로컬 이미지 → Cloudinary 마이그레이션

로컬에 저장된 이미지를 Cloudinary로 이전하고 DB URL을 업데이트하는 Node.js 스크립트:

```bash
npm install cloudinary mysql2
node upload_to_cloudinary.js
```

```javascript
// upload_to_cloudinary.js
const cloudinary = require('cloudinary').v2;
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

cloudinary.config({
  cloud_name: 'CLOUD_NAME',
  api_key: 'API_KEY',
  api_secret: 'API_SECRET',
  secure: true
});

async function main() {
  const conn = await mysql.createConnection({ /* Railway MySQL 접속 정보 */ });
  const [rows] = await conn.execute('SELECT id, image_url FROM product_images');

  for (const row of rows) {
    const filename = row.image_url.replace('/uploads/', '');
    const localPath = path.join(uploadsDir, filename);
    const result = await cloudinary.uploader.upload(localPath, { folder: 'jupjup' });
    await conn.execute('UPDATE product_images SET image_url = ? WHERE id = ?',
      [result.secure_url, row.id]);
  }
  await conn.end();
}
main();
```

---

## 3. 프론트엔드 배포 (Vercel)

### 3-1. GitHub 레포 준비
프론트엔드는 별도 레포로 분리해서 배포:

```bash
cd frontend
git init
git add .
git commit -m "initial commit"
git branch -M main
git remote add origin https://github.com/{username}/jupjup-frontend.git
git push -u origin main
```

### 3-2. Vercel 프로젝트 생성
1. `vercel.com` 접속 → GitHub 로그인
2. **Add New → Project** → `jupjup-frontend` 레포 선택
3. **Framework Preset** → Vite 자동 감지
4. **Deploy** 클릭

### 3-3. 환경변수 설정
**Settings → Environment Variables**에 추가:

```
VITE_API_BASE_URL = https://web-production-fd4e2.up.railway.app
```

설정 후 **Redeploy** 필수!

### 3-4. api/index.js — 환경변수로 baseURL 설정
```javascript
const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}/api`,
})
```

### 3-5. WebSocket URL 환경변수 적용
```javascript
webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_BASE_URL}/ws?token=${token}`)
```

### 3-6. 이미지 URL 처리 (getImageUrl 유틸)
```javascript
export const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url  // Cloudinary URL은 그대로
  return `${import.meta.env.VITE_API_BASE_URL}${url}`  // 로컬 URL은 백엔드 도메인 추가
}
```

---

## 4. 데이터베이스 마이그레이션

### 로컬 DB → Railway MySQL 이전
```bash
# 1. 로컬 DB 덤프
mysqldump -u root -p{비밀번호} jupjup > jupjup_backup.sql

# 2. Railway MySQL에 임포트
mysql -h thomas.proxy.rlwy.net -P {PORT} -u root -p{비밀번호} railway < jupjup_backup.sql
```

---

## 5. 트러블슈팅

### 문제 1 — `no main manifest attribute` 오류
**원인**: plain jar(`-plain.jar`)를 실행하려는 문제
**해결**: `build.gradle`에 `jar { enabled = false }` 추가 후 재배포

### 문제 2 — MySQL 연결 실패 (`CommunicationsException`)
**원인**: 환경변수가 설정되지 않아 localhost로 연결 시도
**해결**: Railway Variables에 `SPRING_DATASOURCE_*` 환경변수 추가

### 문제 3 — CORS 오류 (로그인 후 상품 목록 사라짐)
**원인**: `SecurityConfig`에 CORS 설정 누락
**해결**: `SecurityConfig`에 `corsConfigurationSource()` Bean 추가

### 문제 4 — 이미지 404 오류
**원인**: 로컬 `uploads/` 폴더 파일이 Railway 서버에 없음
**해결**: Cloudinary 연동 후 `upload_to_cloudinary.js` 스크립트로 기존 이미지 마이그레이션

### 문제 5 — 환경변수 적용 안 됨
**원인**: Vercel에서 환경변수 추가 후 Redeploy를 하지 않음
**해결**: 환경변수 변경 후 반드시 **Redeploy** 필요

---

## 6. 배포 후 체크리스트

- [x] Railway 백엔드 배포 완료 (Active 상태)
- [x] Railway MySQL 연결 완료
- [x] Cloudinary 이미지 업로드 연동
- [x] 기존 이미지 Cloudinary 마이그레이션
- [x] Vercel 프론트엔드 배포 완료
- [x] CORS 설정 완료
- [x] 로컬 DB 데이터 Railway MySQL 이전
- [x] 로그인 / 상품 목록 / 채팅 / 찜하기 동작 확인
