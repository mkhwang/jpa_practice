# JPA Query Hint

## 0. Why ?
- `JpaSpecificationExecutor` 인터페이스를 살펴보던 중에 `QueryHints` 를 발견
- QueryHints는 Database에 특정 인덱스 를 사용하도록 힌트를 줄 수 있는 기능이라고 알고있었는데, 그 기능이 아니였음
- 그래서 자세하게 알아보게 됨

## 1. QueryHints
- JPA 쿼리 실행 시 JPA 구현체(주로 Hibernate)에게 특정한 힌트를 주어 동작을 제어하는 기능
- 성능 최적화, 쿼리 캐싱, 읽기 전용 최적화 등에 활용
- `@QueryHints` 또는 `@QueryHint` 어노테이션을 통해 특정 쿼리 메서드에 힌트(Hint)를 부여

```java
@QueryHints(value = {
@QueryHint(name = "org.hibernate.readOnly", value = "true")
})
List<Product> findByName(String name);
```

## 2. 표준 JPA 힌트 (javax.persistence.*)
- JPA 명세에 정의된 힌트
- 모든 구현체에서 공통적으로 지원

| **힌트 이름**                            | **설명**                                 |
| ------------------------------------ | -------------------------------------- |
| javax.persistence.query.timeout      | 쿼리 최대 실행 시간(ms 단위)                     |
| javax.persistence.fetchgraph         | 특정 엔티티 그래프를 따라 fetch (Lazy → Eager 전환) |
| javax.persistence.loadgraph          | fetchgraph와 유사하지만 기본은 유지하고 override    |
| javax.persistence.lock.timeout       | Lock 획득 대기 시간 설정                       |
| javax.persistence.cache.storeMode    | 2차 캐시 저장 방식 (USE, BYPASS, REFRESH)     |
| javax.persistence.cache.retrieveMode | 2차 캐시 조회 방식 (USE, BYPASS)              |

## 3. Hibernate 전용 힌트 (org.hibernate.*)

- Hibernate를 사용하는 경우에만 적용

| **힌트 이름**                             | **설명**                              | **실무 활용**              |
| ------------------------------------- | ----------------------------------- | ---------------------- |
| org.hibernate.readOnly                | 읽기 전용으로 로딩 (dirty checking 생략)      | 대량 조회                  |
| org.hibernate.fetchSize               | JDBC fetch size 설정                  | 대량 처리 (stream/cursor)  |
| org.hibernate.timeout                 | Hibernate 자체 쿼리 timeout (초 단위)      | JDBC timeout과 별도       |
| org.hibernate.comment                 | 쿼리에 주석 추가 (/* comment */)           | 디버깅, APM 분석            |
| org.hibernate.cacheable               | 2차 캐시 사용 여부                         | Entity/쿼리 캐싱           |
| org.hibernate.cacheRegion             | 2차 캐시 영역 지정                         | 캐시 전략 분리               |
| org.hibernate.cacheMode               | NORMAL, GET, PUT, REFRESH 등         | 캐시 동작 세부 제어            |
| org.hibernate.flushMode               | 쿼리 실행 전 flush 전략                    | AUTO, MANUAL, COMMIT 등 |
| org.hibernate.readOnlyEntitiesEnabled | 엔티티 전체를 read-only 처리 (Hibernate 6+) | 읽기 위주의 서비스             |
| org.hibernate.jdbc.batch_size         | JDBC 배치 처리 개수                       | 쓰기 성능 최적화              |

## 4. 언제 써야 하나?

| **힌트 이름**                             | **설명**                              | **실무 활용**              |
| ------------------------------------- | ----------------------------------- | ---------------------- |
| org.hibernate.readOnly                | 읽기 전용으로 로딩 (dirty checking 생략)      | 대량 조회                  |
| org.hibernate.fetchSize               | JDBC fetch size 설정                  | 대량 처리 (stream/cursor)  |
| org.hibernate.timeout                 | Hibernate 자체 쿼리 timeout (초 단위)      | JDBC timeout과 별도       |
| org.hibernate.comment                 | 쿼리에 주석 추가 (/* comment */)           | 디버깅, APM 분석            |
| org.hibernate.cacheable               | 2차 캐시 사용 여부                         | Entity/쿼리 캐싱           |
| org.hibernate.cacheRegion             | 2차 캐시 영역 지정                         | 캐시 전략 분리               |
| org.hibernate.cacheMode               | NORMAL, GET, PUT, REFRESH 등         | 캐시 동작 세부 제어            |
| org.hibernate.flushMode               | 쿼리 실행 전 flush 전략                    | AUTO, MANUAL, COMMIT 등 |
| org.hibernate.readOnlyEntitiesEnabled | 엔티티 전체를 read-only 처리 (Hibernate 6+) | 읽기 위주의 서비스             |
| org.hibernate.jdbc.batch_size         | JDBC 배치 처리 개수                       | 쓰기 성능 최적화              |


## 5. 내가알던 쿼리힌트는 JPA에서 적용가능한가?

- JPA 자체는 DB 인덱스 힌트를 공식적으로 지원하지 않음
- JPA 표준 (javax.persistence)에는 USE INDEX, FORCE INDEX 같은 DB 최적화 힌트를 전달할 수 있는 명세가 없다
- 대신, Hibernate 같은 구현체 수준에서 일부 네이티브 SQL 혹은 특정 확장을 통해 우회할 수는 있음


### 5-1. 네이티브 쿼리에서 직접 사용

```java
@Query(
value = "SELECT * FROM product USE INDEX (idx_category_price) WHERE category = :category AND price > :price",
nativeQuery = true
)
List<Product> findByCategoryAndPrice(@Param("category") String category, @Param("price") int price);
```

- DB 힌트를 직접 작성 가능
- 엔티티 매핑은 자동 안됨 (조인, 서브쿼리 등 제약 있음)
- DB 종속적 (MySQL의 USE INDEX / Oracle의 HINT / PostgreSQL의 GUC 등)

### 5-2. Hibernate @QueryHint(name = "org.hibernate.comment", ...) + DB 옵티마이저 힌트

```java
@Query("SELECT p FROM Product p WHERE p.category = :category")
@QueryHints(@QueryHint(name = "org.hibernate.comment", value = "+ INDEX(idx_category_price)"))
List<Product> findByCategory(@Param("category") String category);
```

- Hibernate는 SQL을 생성할 때 /*+ INDEX(idx_category_price) */ 이런 식으로 주석을 삽입함
- 일부 DB (특히 Oracle)는 주석 기반 힌트를 옵티마이저가 인식함
- 단점
  - MySQL, PostgreSQL에서는 보통 주석 힌트를 인식하지 않음
  - JPA에서 SQL 주석에 옵티마이저 힌트를 넣는 방식은 명확하지 않고 비표준


### 5-3. EntityManager.createNativeQuery() 사용

```java
String sql = "SELECT /*+ INDEX(p idx_category_price) */ * FROM product p WHERE category = :category";
List<Product> result = em.createNativeQuery(sql, Product.class)
.setParameter("category", "electronics")
.getResultList();
```

- 완전 수동 제어 가능
- Hibernate 캐시, 변경 감지 등은 일부 기능 제외됨

### 5-4. 결론

| **힌트 목적**                         | **JPA 지원 여부**              | **비고**               |
| --------------------------------- |----------------------------| -------------------- |
| 읽기 전용 (readOnly)                  | 표준+Hibernate               | 실무에서 많이 사용           |
| 실행시간 제한 (timeout)                 | 표준                         | JDBC 수준 설정           |
| 옵티마이저 힌트 (USE INDEX, /*+ HINT */) | JPA 표준 불가. Hibernate 일부 우회 | nativeQuery 또는 주석 활용 |
| JDBC fetch size 등                 | Hibernate                  | 대량 배치에 유용            |

- JPA는 DB 옵티마이저 힌트(USE INDEX, FORCE INDEX)를 직접적으로 지원하지 않지만
- 네이티브 쿼리 또는 Hibernate 우회 기능으로 부분적으로 구현은 가능하다