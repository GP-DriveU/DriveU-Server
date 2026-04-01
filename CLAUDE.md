# CLAUDE.md — DriveU Server

## 프로젝트 개요

Spring Boot 3.4.5 / Java 21 기반 파일·노트 관리 서버.
MySQL + JPA, AWS S3, OAuth2(Google) + JWT, OpenAI 연동.

---

## 상세 규칙 위치

| 주제 | 파일 |
|------|------|
| 아키텍처 · 레이어 · 패키지 구조 | [.claude/rules/architecture.md](.claude/rules/architecture.md) |
| 네이밍 · DTO · 응답 형식 컨벤션 | [.claude/rules/conventions.md](.claude/rules/conventions.md) |
| 에러 처리 패턴 | [.claude/rules/error-handling.md](.claude/rules/error-handling.md) |
| 반복 안티패턴 | [.claude/rules/anti-patterns.md](.claude/rules/anti-patterns.md) |

---

## 빌드 & 실행 커맨드

```bash
# 빌드
./gradlew build

# 로컬 실행 (dev 프로필)
./gradlew bootRun

# 테스트
./gradlew test

# Docker 이미지 빌드
docker build -t driveu-server .
```

> `application.yml`의 `spring.profiles.active: dev` — 현재 단일 프로필만 확인됨.

---

## 데이터베이스

- MySQL 8.x, 로컬 포트 3306
- Flyway 마이그레이션 활성화 (`baseline-on-migrate: true`)
- `ddl-auto: update` — 프로덕션 환경에서 변경 필요 [TODO: 확인 필요]
- 소프트 삭제 전략: `BaseEntity`의 `isDeleted`, `deletedAt` 필드 사용

---

## 테스트 현황

- 테스트 파일 4개 (소스 166개 대비 매우 낮은 커버리지)
- `@SpringBootTest` 통합 테스트 위주, 단위 테스트 거의 없음
- `src/test/java/com/driveu/server/`에 위치