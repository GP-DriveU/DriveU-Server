# 📁 DriveU Server

> **2025 졸업 프로젝트** — 학부생을 위한 생성형 AI 기반 클라우드 학습 자료 아카이빙 플랫폼

DriveU는 강의 노트, 파일, 링크 등 학습 자료를 학기별 디렉토리 구조로 통합 관리하고, OpenAI를 연동한 **생성형 AI로 노트 요약 및 문제 자동 생성**을 제공하는 서비스입니다.

---

## 📐 Cloud Architecture

> AWS Seoul Region(ap-northeast-2) 기반의 고가용성 인프라

<img width="1100" height="900" alt="image" src="https://github.com/user-attachments/assets/921c9140-f42e-4ded-afda-5222596947d8" />

### 아키텍처 구성 요소

| 구성 요소 | 설명 |
|---|---|
| **VPC** | `10.11.0.0/16` 대역의 전용 네트워크 (`DriveU-vpc`) |
| **Availability Zone** | `ap-northeast-2a`, `ap-northeast-2c` 멀티 AZ 구성 |
| **Public Subnet** | Bastion Host, ALB 위치 (`10.11.0.0/24`, `10.11.1.0/24`) |
| **Private Subnet** | 애플리케이션 서버 위치 (`10.11.10.0/24`, `10.11.11.0/24`) |
| **IGW** | 인터넷 트래픽 진입점 |
| **WAF** | ALB 앞단 웹 방화벽, 악성 트래픽 필터링 |
| **ALB (DriveU-alb)** | 트래픽 로드밸런싱, SSL Termination |
| **DriveU-Server** | Private Subnet 내 Spring Boot 애플리케이션 (Docker 컨테이너) |
| **Bastion Host** | Dev/Ops의 SSH 점프 서버 (Public Subnet) |
| **Amazon RDS** | MySQL — 파일 메타데이터, 유저 정보 저장 |
| **Amazon S3** | 실제 파일 저장소 (Presigned URL 기반 직접 업로드) |
| **AWS CloudFront** | 프론트엔드 빌드 결과물 CDN 배포 |
| **OpenAI API** | GPT 연동 — AI 요약 및 문제 생성 |

### 네트워크 플로우

```
Internet  ──▶  IGW  ──▶  WAF  ──▶  ALB (Public)  ──▶  DriveU-Server (Private)  ──▶  RDS / S3
Dev/Ops   ──▶  IGW  ──▶  Bastion (Public)  ──SSH──▶  DriveU-Server (Private)
Frontend  ──▶  CloudFront  ──▶  S3 (빌드 결과물)
```

---
## 📊 ERD
- `resource` 테이블을 부모로 `file` / `note` / `link`를 분리한 JPA 조인 상속 구조
- Closure Table 기반 디렉토리 계층 임의 깊이의 트리를 단일 쿼리로 조회 가능
<img width="1210" height="1075" alt="image" src="https://github.com/user-attachments/assets/a46b6d15-067c-46c9-95d6-e398c8b872fa" />

---
## 🗄 DB Index Strategy

> Repository 쿼리 패턴을 분석하여 실제 조회에 사용되는 컬럼 조합에 복합 인덱스를 적용, 풀 테이블 스캔을 제거했습니다.

5개 엔티티에 총 10개의 인덱스를 추가했습니다.

| 테이블 | 인덱스 컬럼 | 사용 쿼리 |
|---|---|---|
| `directory_hierarchy` | `(ancestor_id, depth)` | 하위 디렉토리 탐색 |
| `directory_hierarchy` | `(descendant_id, depth)` | 상위 디렉토리 탐색 |
| `resource` | `(is_deleted, updated_at)` | 최근 리소스 목록 조회 |
| `resource` | `(is_deleted, is_favorite, updated_at)` | 즐겨찾기 목록 조회 |
| `resource` | `(is_deleted, deleted_at)` | 휴지통 자동 삭제 스케줄러 |
| `directory` | `(user_semester_id, is_deleted)` | 디렉토리 트리 조회 |
| `directory` | `(is_deleted, deleted_at)` | 휴지통 자동 삭제 스케줄러 |
| `user_semester` | `(user_id, is_deleted)` | 학기 목록 조회 |
| `user_semester` | `(user_id, is_current, is_deleted)` | 현재 학기 조회 |
| `user` | `email` (UNIQUE) | 로그인 시 이메일 조회 |

> **Note:** `directory_hierarchy`의 `ancestor_id`, `descendant_id`는 FK가 아닌 `@Column`으로 선언되어 인덱스가 전혀 없었습니다. 디렉토리 트리를 그리는 모든 요청에서 풀 스캔이 발생하던 문제를 해결했습니다.


---

## 🛠 Tech Stack

### Backend
| 분류 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| ORM | Spring Data JPA + Flyway |
| Security | Spring Security, JWT (JJWT 0.11.5) |
| Auth | OAuth2 (Google) |
| Async | WebFlux (WebClient) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Gradle |

### Infrastructure & Cloud
| 분류 | 기술 |
|---|---|
| Cloud | AWS (Seoul Region) |
| Compute | EC2 (Docker 컨테이너) |
| Database | Amazon RDS (MySQL 8.x) |
| Storage | Amazon S3 (AWS SDK v1 + v2) |
| CDN | AWS CloudFront |
| Load Balancer | AWS ALB |
| Security | AWS WAF, VPC, Security Group |
| Container | Docker |

### AI Integration
| 분류 | 기술 |
|---|---|
| LLM | OpenAI GPT (Responses API) |
| 문서 검색 | OpenAI Assistants API v2 (File Search) |
| Prompt Injection 방어 | 커스텀 `PromptFilter` |

---

## 📁 Project Structure

```
src/main/java/com/driveu/server/
├── domain/
│   ├── ai/            # AI 요약·문제 생성 (Facade 패턴)
│   ├── auth/          # OAuth2 Google 로그인, JWT 발급/검증
│   ├── directory/     # 디렉토리 트리 (Closure Table 패턴)
│   ├── file/          # S3 단일/멀티파트 업로드 (Strategy 패턴)
│   ├── link/          # URL 링크 자료 관리
│   ├── note/          # 마크다운 노트 CRUD
│   ├── question/      # AI 문제 생성·제출·채점
│   ├── resource/      # 파일·노트·링크 통합 Resource 관리
│   ├── semester/      # 학기(UserSemester) 관리
│   ├── summary/       # AI 노트 요약 관리
│   ├── trash/         # 휴지통 (Soft Delete + 30일 자동 삭제)
│   └── user/          # 유저 프로필, 마이페이지
├── global/
│   ├── config/        # Security, CORS, Swagger, Async, S3, WebClient 설정
│   ├── entity/        # BaseEntity (Auditing, Soft Delete)
│   ├── scheduler/     # 휴지통 자동 정리 스케줄러
│   └── util/          # JWT 토큰 추출 유틸
└── infra/
    ├── ai/            # OpenAI WebClient, PromptFilter
    └── s3/            # S3 설정
```

---

## ✨ Key Features

### 🗂 디렉토리 구조 관리
- **Closure Table** 패턴으로 무한 깊이의 계층 구조 지원
- Index 기반 최적화: FK로 선언되지 않은 컬럼에 인덱스를 직접 생성하여, 트리 탐색 시 발생하던 성능 병목 해결
- 디렉토리 생성, 이름 변경, 부모 이동, 정렬 순서 변경
- 학기 생성 시 기본 디렉토리 자동 생성 (`학업`, `과목`, `대외활동` 등)

### 📦 파일 업로드 (Strategy 패턴)
- **단일 업로드**: Presigned PUT URL 발급 → 클라이언트가 S3에 직접 업로드
- **멀티파트 업로드**: 대용량 파일 분할 업로드 (파트별 Presigned URL + ETag 조합)
- 사용자별 저장 용량 추적 및 제한 (기본 5GB)

### 🤖 AI 기능 (OpenAI 연동)
- **노트 요약**: OpenAI Responses API 직접 호출 (WebFlux 비동기 처리)
- **문제 생성**: OpenAI Assistants API (File Search) 활용
  1. 파일 업로드 → Thread 생성 → Run 생성 및 상태 폴링 → 메시지 추출 → 임시 파일 삭제
- **PromptFilter**: 시스템 지침 우회 시도 문자열 사전 차단

### 🔐 인증 및 권한
- Google OAuth2 인증 코드 플로우 (직접 구현)
- JWT Access/Refresh Token 발급 (HS256)
- `@IsOwner` AOP 어노테이션으로 리소스 소유자 선언적 검증
- `@LoginUser` 커스텀 ArgumentResolver로 컨트롤러에서 `User` 객체 주입
- Request 범위 내 유저 정보 캐싱으로 중복 DB 조회 방지

### 🗑 휴지통 & 복구
- Soft Delete 기반 휴지통 (30일 보관 후 자동 영구 삭제)
- 디렉토리 삭제 시 하위 리소스 일괄 Soft Delete (동일 `deletedAt` 타임스탬프)
- 개별 리소스 / 디렉토리 단위 복구, 휴지통 비우기
- S3 파일 영구 삭제는 **트랜잭션 커밋 이후 비동기** 처리

### 📊 학기 관리
- 현재 날짜 기반 학기 자동 감지 및 생성
- 학기 생성·수정·삭제 시 `isCurrent` 플래그 자동 업데이트

---

## 🔧 Design Patterns

| 패턴 | 적용 위치 | 목적 |
|---|---|---|
| **Closure Table** | `DirectoryHierarchy` | 계층 디렉토리 효율적 읽기/쓰기 |
| **Strategy** | `FileUploadStrategy` | 단일/멀티파트 업로드 전략 분리 |
| **AOP** | `OwnerAspect` + `@IsOwner` | 선언적 리소스 소유자 검증 |
| **Template Method** | `RequestBodyConverter` | 리소스 타입(FILE/NOTE)별 AI 요청 변환 |

---

## 🚀 Getting Started

### Prerequisites
- Java 21
- Docker
- MySQL 8.x

### Build & Run

```bash
# 빌드
./gradlew clean build

# Docker 이미지 빌드
docker build -t driveu-server .

# 실행
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<RDS_ENDPOINT>/driveu \
  -e SPRING_DATASOURCE_USERNAME=<DB_USER> \
  -e SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD> \
  -e JWT_SECRET=<JWT_SECRET_BASE64> \
  -e OPENAI_SECRET_KEY=<OPENAI_KEY> \
  -e OPENAI_ASSISTANT_ID=<ASSISTANT_ID> \
  driveu-server
```

### Required Environment Variables

| 변수명 | 설명 |
|---|---|
| `SPRING_DATASOURCE_URL` | RDS MySQL 접속 URL |
| `JWT_SECRET` | JWT 서명 비밀키 (Base64 인코딩) |
| `OPENAI_SECRET_KEY` | OpenAI API Key |
| `OPENAI_ASSISTANT_ID` | OpenAI Assistant ID (File Search용) |
| `AWS_ACCESS_KEY` | S3 접근 키 |
| `AWS_SECRET_KEY` | S3 시크릿 키 |
| `AWS_S3_BUCKET` | S3 버킷 이름 |
| `GOOGLE_CLIENT_ID` | Google OAuth2 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 시크릿 |

---

## 📡 API 명세

서버 실행 후 Swagger UI에서 전체 API를 확인할 수 있습니다.

- **Local**: `http://localhost:8080/swagger-ui.html`
- **Production**: `https://api.driveu.site/swagger-ui.html`

### 주요 API 엔드포인트

| 도메인 | 메서드 | 경로 | 설명 |
|---|---|---|---|
| Auth | `GET` | `/api/auth/google` | 구글 로그인 페이지 리다이렉트 |
| Auth | `GET` | `/api/auth/code/google` | OAuth 코드로 JWT 발급 |
| Semester | `POST` | `/api/semesters` | 학기 생성 |
| Semester | `GET` | `/api/semesters/{id}/mainpage` | 메인 페이지 조회 |
| Directory | `GET` | `/api/user-semesters/{id}/directories` | 디렉토리 트리 조회 |
| Directory | `POST` | `/api/user-semesters/{id}/directories` | 디렉토리 생성 |
| File | `GET` | `/api/file/upload` | 단일 업로드 Presigned URL 발급 |
| File | `POST` | `/api/file/upload/multipart/start` | 멀티파트 업로드 시작 |
| File | `POST` | `/api/file/upload/multipart/complete` | 멀티파트 업로드 완료 |
| Resource | `GET` | `/api/directories/{id}/resources` | 디렉토리별 자료 조회 |
| Resource | `DELETE` | `/api/resources/{id}` | 자료 Soft Delete |
| Note | `POST` | `/api/directories/{id}/notes` | 노트 생성 |
| Note | `PATCH` | `/api/notes/{id}/content` | 노트 내용 수정 |
| Summary | `POST` | `/api/v2/note/{id}/summary` | AI 요약 생성 (v2) |
| Question | `POST` | `/api/v2/directories/{id}/questions` | AI 문제 생성 (v2) |
| Question | `POST` | `/api/questions/{id}/submit` | 문제 풀이 제출 및 채점 |
| Trash | `GET` | `/api/trash` | 휴지통 조회 |
| Trash | `DELETE` | `/api/trash` | 휴지통 비우기 |
| Trash | `POST` | `/api/trash/resources/{id}/restore` | 자료 복구 |
| User | `GET` | `/api/mypage` | 마이페이지 조회 |

---

## 👥 Contributors

2025 졸업 프로젝트 팀 — DriveU
