# URL 단축기

Status: ready-for-agent

## Problem Statement

사용자는 긴 URL을 다른 사람과 공유하기 불편하고, 자신이 예전에 만든 단축 링크들이 얼마나 쓰이고 있는지, 어떤 링크를 만들었었는지 추적하고 관리할 방법이 없다.

## Solution

Google 계정으로 로그인해 URL을 단축하고, 계정별 대시보드에서 만든 모든 링크(원본 URL, 클릭 수 포함)를 관리할 수 있는 서비스를 만든다. 단축 링크는 `{계정핸들}/{도메인축약어}/{별칭}` 형태이며, 도메인축약어는 원본 URL에서 자동 추출되고 별칭은 사용자가 직접 정한다. 링크마다 공개/비공개를 설정할 수 있어, 공개로 설정한 링크는 계정 핸들 기반 프로필 페이지에서 누구나 볼 수 있다.

## User Stories

1. As a visitor, I want to log in with my Google account, so that I can create and manage my own short links without managing a separate password.
2. As a new user, I want to set a unique account handle when I sign up, so that my short links and profile page have a memorable identity.
3. As a logged-in user, I want to paste a long URL and get a shortened link, so that I can share it more easily.
4. As a logged-in user, I want the domain prefix of my short link to be automatically derived from the original URL's host, so that I don't have to type it manually.
5. As a logged-in user, I want to override the automatically derived domain prefix, so that I can customize it when the default isn't what I want.
6. As a logged-in user, I want to choose my own alias for the short link, so that the link is meaningful and memorable (e.g. `op/Faker-KR1`).
7. As a logged-in user, I want to be warned if my chosen domain-prefix + alias combination is already taken on my account, so that I can pick a different one.
8. As anyone with a short link, I want clicking it to redirect me to the original URL, so that I reach the intended destination.
9. As a logged-in user, I want each short link's original URL and alias to be permanently fixed once created, so that shared links don't silently change destination.
10. As a logged-in user, I want to add a new alias for a URL I've already shortened, so that I can create alternate short links without disturbing the existing one.
11. As a logged-in user, I want to delete a short link I no longer need, so that it stops appearing in my dashboard and stops redirecting.
12. As a logged-in user, I want a deleted alias to become available for reuse, so that I'm not permanently blocked from using a short, memorable name again.
13. As a logged-in user, I want to view a dashboard of all my short links, so that I can manage everything I've created in one place.
14. As a logged-in user, I want my dashboard to show the original URL, short link, creation date, and click count for each entry, so that I can see how well each link is doing.
15. As a logged-in user, I want to set each short link as public or private, so that I can control what others can see about my activity.
16. As a logged-in user, I want new short links to default to private, so that I don't accidentally expose something I didn't mean to share.
17. As a logged-in user, I want to switch a short link's visibility between public and private after creation, so that I can change my mind without recreating the link.
18. As any visitor, I want to view a user's public profile page (by their handle), so that I can browse the short links they've chosen to share.
19. As any visitor, I want a user's private short links to be excluded from their public profile page, so that private activity isn't exposed.
20. As a logged-in user, I want to generate a QR code for any of my short links, so that I can share it in offline/printed contexts.
21. As a logged-in user, I want to download the QR code image for a short link, so that I can use it outside the browser.
22. As anyone, I want clicking a short link to issue an HTTP 302 (temporary) redirect, so that the browser always requests the latest destination rather than caching it permanently.
23. As anyone, I want each click on a short link to increment its click count, so that the owner's dashboard analytics stay accurate.
24. As a logged-in user, I want links I delete to be soft-deleted rather than permanently erased, so that the system retains the record internally even though it's hidden from me and others.

## Implementation Decisions

- **Module**: `back/` — Kotlin + Spring Boot 4.0.6, JDK 25, Gradle Kotlin DSL(kts).
- 루트 패키지 `com.back`, 메인 클래스 `com.back.BackApplication`에 `@EnableJpaAuditing` 적용.
- 모든 JPA 엔티티는 `BaseEntity`를 상속한다. `BaseEntity`는 `@CreatedDate`/`@LastModifiedDate`(Spring Data JPA Auditing)로 생성일/수정일을 자동 관리한다.
- 뷰 레이어: Thymeleaf(서버사이드 렌더링). 별도 SPA/API 레이어 없이 컨트롤러가 뷰를 직접 렌더링한다.
- 스타일: Tailwind CSS 4.x.
- 인증: Spring Security OAuth2 Client를 통한 Google 로그인만 지원.
- DB: PostgreSQL, Spring Data JPA/Hibernate로 접근.
- **Account**: 로그인 계정. 고유한 `handle`(가입 시 사용자가 직접 설정)을 가짐. `BaseEntity` 상속.
- **ShortLink**: `originalUrl`, `domainPrefix`, `alias`, 소유 `Account`, `visibility`(공개/비공개, 기본 비공개), `deleted`(소프트 삭제 플래그), `clickCount`. `BaseEntity` 상속. 생성 후 `originalUrl`/`domainPrefix`/`alias`는 불변([ADR 0001](../../docs/adr/0001-immutable-shortlinks.md) 참고).
- **유일성 제약**: `(account, domainPrefix, alias)` 조합은 삭제되지 않은(active) 레코드 사이에서만 유일. 소프트 삭제된 레코드는 이 제약에서 제외되어 재사용 가능.
- **도메인축약어 자동 추출**: 원본 URL 호스트에서 TLD와 흔한 서브도메인(`www` 등)을 제거하고 메인 라벨을 기본값으로 사용. 생성 시 사용자가 직접 덮어쓸 수 있음.
- **리다이렉트**: `GET /{handle}/{domainPrefix}/{alias}` → 302(임시) 리다이렉트로 `originalUrl`로 이동, 요청마다 `clickCount` 증가.
- **대시보드**: `GET /dashboard` (인증 필요) — 로그인 계정 소유의 전체(공개+비공개) `ShortLink` 목록을 원본 URL, 단축 링크, 생성일, 클릭 수와 함께 표시. 항목별 QR 코드 보기/다운로드 액션 포함.
- **프로필 페이지**: `GET /{handle}` (인증 불필요) — 해당 계정의 공개(`visibility=public`) 링크만 표시.
- **QR 코드**: 단축 링크 전체 URL을 인코딩한 이미지를 서버에서 생성(JVM QR 라이브러리 사용, 예: ZXing). 대시보드 항목별로 보기/다운로드 엔드포인트 제공.

## Testing Decisions

- 좋은 테스트는 컨트롤러(HTTP) 경계를 통과하는 통합 테스트다. 내부 협력 객체를 모킹하지 않는다.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `MockMvc`(또는 동급 HTTP 클라이언트)로 실제 HTTP 요청/응답을 검증한다.
- DB는 Testcontainers로 띄운 실제 PostgreSQL을 사용한다. DB를 모킹하지 않는다.
- 외부 경계인 Google OAuth만 모킹/스텁 처리한다(Spring Security 테스트 지원으로 인증된 테스트 사용자를 사전 설정).
- 대상 모듈: 계정/로그인 컨트롤러, 단축 링크 생성 컨트롤러, 리다이렉트 컨트롤러, 대시보드 컨트롤러, 프로필 페이지 컨트롤러, QR 코드 엔드포인트.
- HTTP 상태 코드, 리다이렉트 시 `Location` 헤더, 렌더링된 Thymeleaf 응답 본문 내용을 검증한다.
- 그린필드 프로젝트라 기존 테스트 사례 없음 — 이번에 작성하는 테스트가 이후 작업의 기준 패턴이 된다.

## Out of Scope

- 클릭 시간대/유입 경로/지역 등 상세 분석
- 링크 만료(TTL), 비밀번호 보호 등 추가 보안 기능
- Google 외 다른 소셜 로그인 제공자
- 도메인축약어/별칭의 문자 제한, 길이 등 세부 유효성 규칙(구현 시 합리적 기본값 적용)
- Rate limiting / 남용 방지 정책
- 관리자 기능(타 계정 관리, 신고 처리 등)

## Further Notes

- 용어 정의는 `CONTEXT.md`, 불변성 관련 결정은 `docs/adr/0001-immutable-shortlinks.md` 참고.
- 프론트엔드는 Thymeleaf 서버 렌더링이므로 별도 API/SPA 레이어를 두지 않는다.
