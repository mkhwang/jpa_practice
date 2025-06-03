# Incremental Sort(증분정렬)

## Why ? 
- 블로그를 보다가 증분정렬에 관련된 내용을 봐서 자세하게 알아보기로 하자

## What is Incremental Sort ?
- 정렬 조건 중 일부가 이미 정렬되어 있을 경우,  나머지 조건만 부분 정렬(in-memory sort)하여 정렬 비용을 줄이는 방식
- 즉, 전체 데이터를 한꺼번에 정렬하지 않고,  선행 정렬된 키(presorted key) 기준으로 작은 그룹들로 나누어 정렬

### 기존 정렬과의 차이

|항목|기존 정렬 (Sort)|Incremental Sort|
|---|---|---|
|정렬 대상|전체 데이터|선행 키 단위의 작은 그룹들|
|메모리 부담|큼|작음|
|성능|느릴 수 있음|빠름 (특히 큰 테이블에서)|
|전제 조건|인덱스가 정렬 조건과 완전히 일치해야 최적화 가능|일부만 정렬되어 있어도 OK|

## Examples

### 설정
#### 인덱스

```
CREATE INDEX idx_orders_created ON orders (created_at);
```

#### 쿼리

```
SELECT * FROM orders
WHERE created_at >= '2023-01-01'
ORDER BY created_at, id;
```

- 여기서 인덱스는 created_at만 정렬돼 있음
- 하지만 정렬 조건은 created_at, id
- PostgreSQL 12까지는 → 전체 정렬 필요
- PostgreSQL 13+부터는 → Incremental Sort 사용


### 실행 계획 비교

#### PostgreSQL 12 이전

```
Sort  (cost=...)
  Sort Key: created_at, id
  ->  Index Scan using idx_orders_created on orders ...
```

- 전체 데이터를 정렬해야 하므로 메모리 사용↑ / 느림

#### PostgreSQL 13 이후 (Incremental Sort 적용 시)

```
Incremental Sort  (cost=...)
  Sort Key: created_at, id
  Presorted Key: created_at
  ->  Index Scan using idx_orders_created on orders ...
```

- created_at 기준으로는 이미 정렬됨
- 같은 created_at을 가진 그룹만 id로 메모리 정렬
- → 성능 대폭 향상 가능

### 동작 조건 요약

|조건|Incremental Sort 가능 여부|
|---|---|
|인덱스가 a이고 정렬 조건이 a, b| 가능 (a는 Presorted Key)|
|인덱스가 b, 정렬 조건이 a, b| 불가능 (선두 키 불일치)|
|정렬 조건이 a, b, c, 인덱스는 a, b| 가능 (a, b presorted, c만 정렬)|


```
SET enable_incremental_sort = off;
```

## TIPS

- ORDER BY a, b, 인덱스가 a일 경우 → Incremental Sort 유도 가능
- LIMIT과 함께 쓰면 효과 극대화 (불필요한 전체 정렬 제거)
- EXPLAIN ANALYZE에서 Incremental Sort, Presorted Key 확인
- 정렬 조건을 잘 설계하면, 완전 정렬 인덱스 없어도 성능 향상 가능

## Conclusion

> Incremental Sort는 PostgreSQL이 일부 정렬 조건만 인덱스로 만족할 때, 나머지를 효율적으로 처리하기 위한 핵심 성능 기능

- ORDER BY 조건의 선두 컬럼이 인덱스에 포함되어 있으면 자동으로 적용될 수 있고,
- 대용량 테이블의 정렬 성능을 수배 이상 개선할 수 있음

## 번외

### Q. LIMIT과 함께 쓰면 효과 극대화 ?

- 페이징을 위해 정렬이 다 되고 난 다음에 limit 으로 특정 row만 가져오는데, 왜 극대화 되는거지 ?

### A. 정렬 알고리즘이 LIMIT을 고려해서 더 똑똑하게 작동하기 때문

- 정렬은 LIMIT 이후가 아닌, 이전에 반드시 수행 됨

쿼리 예시:

```
SELECT * FROM orders
WHERE created_at >= '2023-01-01'
ORDER BY created_at DESC, id DESC
LIMIT 10;
```

#### 실행 순서:

1. WHERE 조건으로 row 필터링
2. ORDER BY로 정렬
3. 정렬된 결과에서 LIMIT 만큼 잘라냄

**즉, LIMIT은 결과를 자르기만 할 뿐, 정렬은 이미 끝나 있어야 함**

#### 그럼에도 “LIMIT과 함께 쓰면 효과적”인 이유 ?
##### 이유 1:
1. 정렬 알고리즘이 “필요한 만큼만 정렬”하는 방식으로 최적화됨 
   2. PostgreSQL에서는 LIMIT이 있는 경우, quicksort나 top-N heapsort, 또는 Incremental Sort가 “전체를 정렬하지 않고도 결과를 뽑을 수 있다면 그렇게 하라”는 힌트를 옵티마이저에게 줍니다.

- 정렬 대상이 10만 건이지만,
- LIMIT 10이면 → 상위 10건만 있으면 나머지 안 봐도 됨
- 이럴 때 Incremental Sort는 미리 정렬된 컬럼 기반으로 그룹별로 탐색하며, 상위 N건만 빠르게 확보

✔️ 결국 정렬 비용이 급감함

##### 이유 2:
- Incremental Sort는 작은 정렬 그룹들을 빠르게 정리할 수 있음

예를 들어:

- 인덱스가 (created_at)으로 정렬돼 있고
- 쿼리는 ORDER BY created_at, id LIMIT 10


이 경우:

- created_at이 presorted key
- 동일한 created_at 값들만 작게 메모리에 올려서 id 정렬
- LIMIT에 도달하는 순간 → 정렬 중단 가능

→ 전체 10만건 중 1,000건만 정렬하고 LIMIT 도달하면 그걸로 끝.

#### Conclusion

- LIMIT은 정렬 이후에 실행되지만,
- 옵티마이저는 LIMIT을 감안하여 정렬 전략을 더 빠르게 바꿈
- Incremental Sort는 이 전략 중 하나로, 필요한 부분만 정렬해서 LIMIT 도달 시 종료

> 그래서 정렬 + LIMIT 조합에서는 Incremental Sort가 성능상 매우 강력한 것
