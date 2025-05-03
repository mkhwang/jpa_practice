# Spring data history

## 0. Why?
- org.springframework.data.geo package를 보다가 history라는 패키지를 발견함
- Spring Data에서 제공하는 기능인지 궁금해서 찾아보게 됨

## 1. Spring Data History

- 사용하려면 `spring-data-envers` 의존성과 함께 `@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)` 등의 설정이 필요
- org.springframework.data.history.Revision은 Spring Data에서 감사(Auditing) 또는 버전 관리(Versioning)을 할 때 사용하는 객체
- 하나의 `리비전(revision)` 정보를 담는 컨테이너 역할
- `Revision<N, T>` 는 다음과 같은 제네릭 구조
  - `Revision<RevisionNumber, Entity>`
    - ReisionNumber (N) — 리비전 번호 (Integer, Long, 또는 커스텀 타입)
    - Entity (T) — 특정 시점의 엔티티 스냅샷
  - 즉, “N번째 수정본의 Entity 상태”를 의미

### 1-1. 언제 사용하나?
- 이건 Spring Data Envers나 Spring Data JDBC의 Revision 기능과 함께 사용
- Hibernate Envers 기반으로 엔티티 변경 이력을 관리할 때,
- ```java
    @Audited
    @Entity
    public class Product {
      @Id
      private Long id;
      private String name;
    }
    ```
- 조회
    ```java
    @Autowired
    private RevisionRepository<Product, Long, Integer> repository;
    
    List<Revision<Integer, Product>> revisions = repository.findRevisions(productId);
    ```

### 1-2. 실제 Revision 객체 내용

```java
Revision<Integer, Product> rev = ...
rev.getRevisionNumber(); // 1, 2, 3, ...
rev.getEntity();         // 수정 당시의 Product 엔티티
rev.getMetadata();       // 수정 시점, 수정자 등 메타데이터
```

## 2. Envers 없이도 사용 가능?

- `org.springframework.data.history.Revision` 자체는 envers 없이도 존재하지만, 실제 Revision 기능을 제대로 쓰려면 Envers 같은 구현체가 필요

### 2-1. Revision은 Spring Data Common에 정의돼 있는 인터페이스성 개념 객체
- 즉, “변경 이력”을 다룰 때 사용할 수 있는 일반적인 컨테이너지, 특정한 동작을 혼자서 구현 X
- Revision<T, N> 은 단지 이런 정보를 담기 위한 표준 인터페이스
- getEntity() — 그 시점의 엔티티
- getRevisionNumber() — 리비전 번호
- getMetadata() — 작성자, 시각 등 메타정보



### 2-2. Revision 기능은 누가 실제 구현하냐?
- Spring Data Common은 기본 스펙만 제공
- Hibernate Envers, Custom Revision Entity 설계, 혹은 Spring Data JDBC의 버전관리 기능 등을 통해 실제 구현해야 함

### 2-3. 그렇다면… 언제 쓸 수 있어?

- RevisionRepository : Envers 있어야 함
- Revision<Integer, T> : 정의는 돼있지만 쓰임 없음	단독으론 의미 없음
- Auditing (작성자/시간) : @CreatedDate, @LastModifiedBy 등 가능
- 버전 관리 (낙관적 락): @Version 필드 사용 가능 (별개 기능)


## 3. 추가

### 3-1. Custom Rollback
```java
Revision<Integer, Product> revision = productRepository.findRevision(id, revisionNumber)
    .orElseThrow(() -> new IllegalArgumentException("Revision not found"));

Product snapshot = revision.getEntity();

Product current = productRepository.findById(id).orElseThrow();

// 예시: 특정 필드만 롤백 (구현필요)
current.restore(snapshot);

// 혹은 전체 덮어쓰기 (구현필요)
current.overwriteAll(snapshot);

productRepository.save(current);
```

- 이전 이력을 그대로 다시 save() 하지 말 것
- 그 이력은 당시의 ID와 상태를 반영한 스냅샷
- 연관 객체나 트랜잭션 상태가 꼬일 수 있음
- 대신, 복구 전용 restore() 메서드를 Aggregate 내부에 만들어서
- 정상적인 상태 전이를 도메인 규칙에 맞게 적용하는 것이 DDD스럽고 안전
- @Version 필드가 있다면, 롤백 시 Optimistic Lock 충돌 가능
- 연관 엔티티가 많은 경우는 연관 객체 상태까지 신중하게 다뤄야 함
- 복구된 버전을 새 Revision으로 남겨야 한다면 → save()만으로 충분

### 3-2. 별도로 Auditing 기능을 구현할 수 있음

- 고급스럽게 설계하면 Spring Data JPA + Domain Event + Event Listener 조합
- 영속성 컨텍스트를 벗어나기 전에(혹은 이후에) 정확히 필요한 시점에 이벤트를 발행하고, 그걸 별도 히스토리 테이블에 저장하는 구조를 만들 수 있음


#### 3-2-1. `엔티티 변경 시마다, 도메인 이벤트를 발행해서 별도 테이블에 변경 이력을 저장하는 구조` 구현 

1. 도메인 이벤트 클래스 정의
```java
public class ProductChangedEvent {
private final Long productId;
private final String name;
private final Instant changedAt;

    // constructor, getter...
}
```

2. 엔티티에서 도메인 이벤트 수집

```java
@Entity
public class Product {

    @Id
    private Long id;
    private String name;

    @Transient
    private final List<Object> events = new ArrayList<>();

    public void changeName(String newName) {
        this.name = newName;
        events.add(new ProductChangedEvent(this.id, newName, Instant.now()));
    }

    @DomainEvents
    public Collection<Object> domainEvents() {
        return events;
    }

    @AfterDomainEventPublication
    public void clearEvents() {
        events.clear();
    }
}
```

- `@DomainEvents` 는 Spring Data JPA가 save() 시점에 자동으로 이벤트 발행해줌
- `@AfterDomainEventPublication` 로 이벤트 큐 정리


3. Event Listener에서 History 테이블에 저장

```java
@Component
public class ProductChangedEventHandler {

    private final ProductChangeHistoryRepository historyRepository;

    public ProductChangedEventHandler(ProductChangeHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @TransactionalEventListener
    public void handle(ProductChangedEvent event) {
        ProductChangeHistory history = new ProductChangeHistory(
            event.getProductId(), event.getName(), event.getChangedAt()
        );
        historyRepository.save(history);
    }
}
```

- `@TransactionalEventListener` 를 쓰면 트랜잭션 커밋 직후에 처리 가능
- 롤백 시 이벤트도 롤백되도록 보장

### 3-3. REVINFO custom

- REVINFO 테이블은 Hibernate Envers에서 리비전 정보를 저장하는 테이블
- 기본적으로 리비전 번호는 int 타입으로 저장됨
- 실제 운영시 int는 부족할 수 있음 -> 리비전 번호를 long 타입으로 변경하고 싶다면, 커스텀 리비전 엔티티를 만들어야 함

1. 커스텀 리비전 엔티티 정의
```java
@Entity
@RevisionEntity
public class CustomRevEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private Long id;

    @RevisionTimestamp
    private long timestamp;

    // getter/setter 생략
}
```

핵심:
- `@RevisionNumber` 필드를 Long으로 선언
- `@GeneratedValue`와 함께 사용 (예: auto-increment)


2. 설정에서 Envers가 이 엔티티를 쓰도록 지정

- application.properties (또는 yaml) 에는 따로 지정 필요 없고,
- Hibernate는 @RevisionEntity가 붙은 클래스를 자동으로 인식해 사용

3. Repository에 long 타입 사용

```java
public interface ProductRevisionRepository
extends RevisionRepository<Product, Long, Long> {
}

```
