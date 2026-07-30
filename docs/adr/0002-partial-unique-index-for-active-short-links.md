# 활성 단축 링크 유일성은 schema.sql의 부분 유니크 인덱스로 강제

`(계정, 도메인축약어, 별칭)` 조합은 소프트 삭제되지 않은(active) 레코드 사이에서만 유일해야 한다 — 삭제된 별칭은 재사용 가능해야 하기 때문이다. JPA `@UniqueConstraint`는 조건부(partial) 인덱스를 표현할 수 없어 전체 레코드(삭제 포함)에 걸리는 무조건 유일 제약만 만들 수 있고, 이는 재사용 요구사항과 충돌한다. 프로젝트에 Flyway/Liquibase 같은 마이그레이션 도구가 없어, `spring.jpa.defer-datasource-initialization=true` + `spring.sql.init.mode=always`로 Hibernate의 `ddl-auto` 스키마 생성 이후 `schema.sql`이 실행되게 하고, 그 안에서 `WHERE deleted = false` 조건의 Postgres 부분 유니크 인덱스를 직접 만든다. 엔티티의 `@Table(uniqueConstraints=...)`는 제거했다.
