# Outbox Pattern

- 메시징 기반 마이크로서비스에서 데이터 정합성과 이벤트 유실 방지를 동시에 해결하는 핵심 설계 기법

---

## Why Outbox Pattern ?

- 마이크로서비스에서 서비스 간 통신을 Kafka, RabbitMQ 같은 메시징 시스템으로 처리할 때 자주 마주치는 문제

> "DB에는 저장됐는데, 이벤트는 전송되지 않았어."

- 이는 흔히 **DB와 메시지 브로커 간의 이중 트랜잭션 문제**로 발생 
- 일반적인 구조에서 다음과 같은 코드가 있다고 가정

```java
orderRepository.save(order); // DB 저장
kafkaTemplate.send("order.created", order); // 이벤트 발행
```

- 이 경우 중간에 Kafka 전송이 실패하면 **DB에는 주문이 저장되었지만, 이벤트는 유실**
- 반대로 Kafka 전송이 먼저 되고 DB가 롤백되면, 유령 이벤트(잘못된 메시지)가 전파
- 이러한 문제를 해결하기 위한 실전 설계가 바로 **Outbox Pattern**

---

## What Outbox Pattern ?

- **발행해야 할 이벤트를 메시지 큐에 바로 보내는 것이 아니라, DB의 별도 테이블(Outbox)에 먼저 저장 나중에 메시지를 발행하는 방식**
- **DB 트랜잭션 안에서 도메인 로직과 이벤트 기록을 함께 처리**
- Kafka 등 외부 시스템으로의 전송은 트랜잭션 외부에서 비동기로 처리

---

## 전체 흐름 요약

```plaintext
[도메인 서비스 실행]
   ↓
[DB 트랜잭션 안에서]
 - 비즈니스 데이터 저장 (예: Order)
 - Outbox 테이블에 이벤트 기록
   ↓
[트랜잭션 커밋]
   ↓
[Scheduler 또는 Kafka Connect로 Outbox 읽기]
   ↓
[Kafka 등 메시지 브로커로 전송]
```

---

## 예시 코드 (Spring 기반)

### 0. 이벤트 발행 및 처리 흐름 개요

```plaintext
[1] OrderService.placeOrder()
  └─ save(order)
  └─ publishEvent(OrderCreatedEvent)

[2] BEFORE_COMMIT
  └─ OutboxMessage 저장
  └─ OutboxMessage published = false

[3] AFTER_COMMIT or Scheduler
  └─ Kafka 전송 (sendService.send())
  └─ OutboxMessage published = true
```

### 1. 도메인 서비스 및 이벤트 발행

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void placeOrder(Order order) {
        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
    }
}
```

### 2. BEFORE\_COMMIT: Outbox 테이블에 이벤트 저장

```java
@Component
@RequiredArgsConstructor
public class OrderOutboxRecorder {

    private final OutboxRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(OrderCreatedEvent event) {
        OutboxMessage message = new OutboxMessage(
            "OrderCreated",
            serialize(event.getOrder()),
            false,
            LocalDateTime.now()
        );
        outboxRepository.save(message);
    }
}
```

### 3. Outbox 엔티티 정의

```java
@Entity
public class OutboxMessage {
    @Id @GeneratedValue
    private Long id;
    private String type;
    private String payload;
    private boolean published = false;
    private LocalDateTime createdAt;
}
```

### 2. 도메인 로직에서 이벤트 저장

```java
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
    outboxRepository.save(new OutboxMessage(
        "OrderCreated",
        serialize(order),
        false,
        LocalDateTime.now()
    ));
}
```

### 4. AFTER_COMMIT: Kafka 메시지 전송

```java
@Component
@RequiredArgsConstructor
public class OrderKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SendService sendService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(OrderCreatedEvent event) {
        sendService.send(OrderEventPayload.from(event));
    }
}
```

### 5. 이벤트 전송 스케줄러 (Outbox polling 방식, 대체 가능)

```java
@Scheduled(fixedDelay = 1000)
public void flushOutbox() {
    List<OutboxMessage> events = outboxRepository.findUnpublished();
    for (OutboxMessage event : events) {
        kafkaTemplate.send("order.events", event.getPayload());
        event.markPublished();
    }
}
```

---

## 장점 정리

| 항목        | 설명                            |
| --------- | ----------------------------- |
| 정합성 보장    | DB 저장과 이벤트 기록이 하나의 트랜잭션으로 처리됨 |
| 재처리 용이    | 전송 실패 시 Outbox에서 재시도 가능       |
| 메시지 유실 방지 | Kafka 전송 실패에 대비해 로그 기반 보존 가능  |
| 구현 난이도 적당 | Debezium 등 외부 도구 없이 직접 구현 가능  |

---

## 고려할 점

| 항목               | 설명                                            |
| ---------------- | --------------------------------------------- |
| Outbox 테이블 정리 정책 | 일정 기간이 지나면 아카이빙 또는 삭제 필요                      |
| 멱등성 보장           | 같은 메시지를 여러 번 처리하지 않도록 Consumer 설계 필요          |
| 스케줄러 장애 대비       | 실패 로그, 알람, 수동 재시도 기능 필요                       |
| 구조 유연화           | 향후 Kafka Connect or Debezium 방식으로 확장 가능하도록 설계 |

---

## 실무 적용 팁

* `eventId` 또는 `aggregateId + version` 등을 함께 저장해 **중복 전송 감지**
* Kafka 헤더에 `eventType`, `sourceSystem`, `traceId` 등을 포함해 **관찰 가능성(Observability)** 강화
* Outbox 테이블을 파티셔닝하거나 별도 스키마로 분리해 **성능 최적화**
* View 모델이 함께 업데이트되어야 한다면 **이벤트 루트 기준으로 선/후처리 명확히 분리**

---

## 결론

- Outbox Pattern 은 마이크로서비스 환경에서 **신뢰할 수 있는 이벤트 기반 시스템을 만들기 위한 기본적인 안전장치**
- 단순한 패턴이지만, 이를 도입함으로써 트랜잭션 정합성, 메시지 유실 방지, 장애 대응의 기반이 크게 향상

- Kafka, RabbitMQ, EventBridge 등 어떤 메시지 브로커를 사용하든 Outbox는 모든 시스템에 적용할 수 있는 범용 설계
- 필요한 경우 Debezium이나 CDC 기반 시스템으로 확장도 가능

> "이벤트 기반 시스템의 신뢰성과 복원력을 높이고 싶다면, Outbox는 필수다."

