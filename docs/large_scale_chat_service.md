# 대규모 채팅 서비스

## 0. Why?
- 소규모 채팅 서비스는 redis pub/sub을 이용해서 간단하게 구현할 수 있다.
- 하지만 대규모 채팅 서비스는 redis pub/sub을 이용해서 구현하기에는 한계가 있다.
- 대규모에서는 어떻게 구현할 수 있을까?

## 1. Event-Driven 아키텍처 + Message Broker
- Kafka, NATS, Pulsar 등을 사용
- 채팅 메시지를 영속 큐에 넣고, 필요한 서비스가 각각 consume
- 장점:
  - 메시지 유실 방지
  - 백프레셔 대응 가능
  - 리플레이/복구 가능
  - 사용자별 또는 방별 파티셔닝 처리 용이

## 2. WebSocket 서버 + 세션 레지스트리 (분산 처리)
- WebSocket 서버는 여러 인스턴스로 확장하고, 클라이언트는 각기 다른 인스턴스에 연결됨
- 사용자의 연결 상태는 Redis 같은 인메모리 저장소로 세션 매핑을 관리
- user_id ↔ server_instance_id
- 메시지를 보낼 때는 해당 유저가 연결된 서버로 라우팅

```text
User A ──> WS Server 1
User B ──> WS Server 3

A가 B에게 메시지를 보내면:
WS Server 1 → Message Broker → WS Server 3 → User B
```

## 3. Room 기반 분산 처리 (Sharding)
- 방 ID 기준으로 샤딩하여 각 방은 하나의 채팅 처리 노드에 고정
- 이를 통해 채팅방별 순서 보장, 처리 병렬화
- 예: 방 ID 해시 기반으로 Kafka 파티션 또는 채팅 노드 선택

## 4. 데이터 저장 구조
- 실시간 채팅은 Redis (short TTL) 또는 Cassandra/MongoDB 등으로 버퍼링
- 영속 저장은 RDBMS 또는 Kafka Sink → DW 등으로 처리
- 메시지 전송 → 수신 → 읽음 처리 → 저장까지 이벤트 체인으로 연결

## 5. 확장성 관점 고려사항
- Backpressure 처리: 채팅 폭주시 큐 관리 필수
- 멀티 디바이스 동기화: 동일 유저가 여러 디바이스에서 접속할 때 싱크
- Offline 메시지 큐: 오프라인 유저에게 메시지 큐잉 후 접속시 푸시
- ACK/Retry 정책: 신뢰성 있는 전송 보장 (QoS 1 수준 구현)
- 보안: 메시지 서명, 암호화, 접근 제어 (방별 권한)

```

[Client] ──WS──> [WS Gateway] ──> [Message Broker] ──> [Chat Processor] ──> [DB]

                                ↘                   ↘
                             [Session Registry]   [Push/Notification Service]


```
