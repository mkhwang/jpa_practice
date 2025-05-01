# JPA와 Postgresql 사용시 LOB 사용시 주의사항

## 1. Dialect
- PostgreSQLDialect 에서는 기본적으로 LOB을 postgresql oci 로 사용한다.
- [OID](https://www.postgresql.org/docs/current/datatype-oid.html) 는 PostgreSQL에서 제공하는 Object Identifier 
  - LOB을 저장할 때, LOB의 위치를 저장하는 OID를 사용한다.
  - LOB을 읽을 때, OID를 사용하여 LOB을 읽어온다.
  - 데이터는 pg_largeobject 라는 테이블에 저장된다.

## 2. JPA에서 TEXT사용 시, OID를 사용하지 않는 방법
### 2-1. Annotation
- `@Lob` 어노테이션을 사용하지말고, `@Column(columnDefinition = "text")` 를 사용해서 OID를 사용하지 않도록 한다.
- 물론 컬럼도 OID type이 아닌 text type으로 정의되어 있어야 한다.
### 2-2. Custom Dialect
- `PostgreSQLDialect` 을 상속받아서, `registerColumnType` 메소드를 오버라이드 한다.

## 3. OID를 그대로 사용할 때 주의사항
### 3-1. LOB을 읽을 때
- JDBC 가 알아서 LOB을 읽어온다.
- Hibernate에서 LOB(특히 PostgreSQL oid)는 JDBC의 Large Object API를 사용해서 트랜잭션 내에서만 읽을 수 있음 → 트랜잭션이 끝난 후에 접근하면 Unable to access lob stream 예외 발생
- 트랜잭션이 끝난 후에 LOB을 읽으려면, `@Transactional(readOnly = true)` 를 사용해야 한다.
- 그리고 auto commit 모드에서는 LOB을 읽을 수 없다.
  - `Large Objects may not be used in auto-commit mode.`

### 3-2. update 시
- LOB값을 update한 경우, 키값이 바뀌고 pg_largeobject 에 새로운 row가 생성되며 매핑된다.
- PostgreSQL에서 @Lob → oid로 매핑된 컬럼을 JPA로 update할 때 기존 행의 LOB 데이터를 “수정”하지 않고,
  새로운 Large Object(= 새로운 oid)를 생성하면서 참조를 바꾸는 이유는 바로 PostgreSQL의 LOB(= Large Object) 처리 방식 때문입니다.
- oid는 pg_largeobject 테이블의 특정 대용량 객체의 ID(참조키)
- 기존 LOB은 명시적으로 지우지 않으면 계속 남아있음 (메모리 누수)
- ```sql
    SELECT lo_unlink(description) FROM products WHERE id = :id;
    ```
  같은 쿼리로 직접 삭제해야함

## 3. 결론
- LOB을 사용할 때는 @Lob 어노테이션을 사용하지 말고, `@Column(columnDefinition = "text")` 를 사용하자.
  - 특수한 경우가 아니면 PostgreSQL에서 OID를 사용하지 말자
- DDL에 text로 정의되어 있고, Dialect를 커스텀하지 않았거나, `@Column(columnDefinition = "text")` 를 사용하지 않았다면, JPA에서는 OID를 사용하여 LOB을 처리한다.