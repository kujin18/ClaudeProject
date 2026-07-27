# 01 — 프로젝트 스캐폴딩 & Google 로그인

**What to build:** Kotlin + Spring Boot 백엔드 프로젝트를 생성하고, 사용자가 Google 계정으로 로그인해 최초 로그인 시 고유한 계정 핸들을 설정할 수 있게 한다.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] `back/` 폴더에 Kotlin, Spring Boot 4.0.6, JDK 25, Gradle Kotlin DSL(kts)로 프로젝트가 생성되어 있다.
- [ ] 루트 패키지는 `com.back`이고 메인 클래스는 `com.back.BackApplication`이며 `@EnableJpaAuditing`이 적용되어 있다.
- [ ] `BaseEntity`가 존재하며 생성일/수정일을 `@CreatedDate`/`@LastModifiedDate`로 자동 기록한다. `Account` 엔티티는 `BaseEntity`를 상속한다.
- [ ] PostgreSQL이 연결되어 있고, 테스트는 Testcontainers로 실제 Postgres에 대해 실행된다.
- [ ] Thymeleaf 템플릿과 Tailwind 4.x 빌드 파이프라인이 동작한다(기본 페이지 렌더링 확인 가능).
- [ ] Google OAuth2 로그인 버튼을 눌러 로그인할 수 있다.
- [ ] 최초 로그인한 사용자는 고유한 계정 핸들을 설정하는 화면을 거친다.
- [ ] 이미 사용 중인 핸들은 다시 선택할 수 없다.
- [ ] 로그인 상태가 세션에 유지되어 이후 요청에서 로그인 사용자를 식별할 수 있다.
