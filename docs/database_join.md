# Database Join

## Why ? 
- 블로그를 찾아보던중, 쿼리 성능 튜닝 과정에서 nested loop join 을 hash join으로 튜닝하는 것으로 성능개선한 내용을 확인
- 그래서 이것이 무엇인지 알아보도록 함

## 1. Nested Loop Join (중첩 반복 조인)

### 개념

- Nested Loop Join은 두 테이블 중 하나를 기준으로 루프를 돌리면서, 다른 테이블을 매번 반복 탐색하는 방식
- 이는 컴퓨터공학에서 흔히 배우는 **이중 for 루프 구조**와 동일하며, 가장 직관적인 조인 방식

###  동작 원리

* 외부 루프 테이블에서 한 행을 선택
* 내부 루프 테이블을 순회하며 매칭되는 행을 탐색
* 조건이 일치하면 조인 결과에 포함

```sql
FOR outer_row IN outer_table LOOP
  FOR inner_row IN inner_table LOOP
    IF outer_row.key = inner_row.key THEN
      RETURN combined_row;
    END IF;
  END LOOP;
END LOOP;
```

### 예시 쿼리

```sql
SELECT *
FROM employees e
JOIN departments d ON e.department_id = d.id;
```

- 이 예시는 employees 테이블을 기준으로 반복하며, departments에서 일치하는 부서 ID를 찾습니다.

### 장점

* 소규모 테이블이거나 **내부 루프에 인덱스가 있다면** 매우 효율적
* **복잡한 조인 조건**이나 조건이 동적으로 주어지는 경우 유연하게 대응 가능
* 구현이 단순하고 이해하기 쉬움

### 단점

* 두 테이블이 모두 클 경우 O(n×m)의 **시간 복잡도로 인해 매우 느림**
* 병렬화에 부적합하며, **쿼리 성능 병목의 주요 원인**이 될 수 있음

###  언제 쓰일까?

* 한쪽 테이블이 아주 작거나, 내부 루프 테이블에 인덱스가 있어 빠르게 조회할 수 있을 때
* 조건이 단순하지 않아 다른 조인 방식이 불리한 경우

---

## 2. Merge Join (병합 조인)

### 개념

- Merge Join은 **두 테이블이 조인 조건의 키로 정렬되어 있을 때** 사용되는 방식
- 정렬된 상태를 이용해 마치 **정렬된 배열을 병합하는 것처럼** 효율적으로 매칭

### 동작 원리

* 두 테이블이 조인 키 기준으로 정렬되어 있어야 함
* 양쪽에 포인터를 두고, 키를 비교하면서 일치 여부 확인
* 일치하면 결과에 포함하고, 그 외에는 작은 쪽 포인터를 이동

```sql
-- 테이블이 정렬된 상태라고 가정
WHILE not at end of both tables LOOP
  IF left.key = right.key THEN
    RETURN join_result;
  ELSIF left.key < right.key THEN
    advance left;
  ELSE
    advance right;
  END IF;
END LOOP;
```

### 예시 쿼리

```sql
SELECT *
FROM orders o
JOIN customers c ON o.customer_id = c.id;
```

- 정렬되어 있는 상태이거나, 정렬 가능한 인덱스가 존재하면 Merge Join이 선택

### 장점

* 정렬된 데이터라면 **한 번의 순회로 빠르게 조인** 가능
* **중복 키가 많을 경우**에도 효율적으로 처리 가능
* 대용량 테이블에도 잘 동작함

### 단점

* 정렬이 필요할 경우 **Sort 연산 비용** 발생
* 정렬 상태가 유지되지 않으면 비효율적

###  언제 쓰일까?

* 조인 키에 대해 정렬되어 있거나 인덱스로 정렬이 가능한 경우
* `ORDER BY`, `LIMIT` 절과 함께 사용되면 시너지 있음

---

## 3. Hash Join (해시 조인)

### 개념

- Hash Join은 한 테이블의 조인 키로 해시 테이블을 만들고, 다른 테이블의 값을 이 해시 테이블에 조회하여 매칭하는 방식
- 주로 인덱스나 정렬이 없는 경우 선택되며, **등호(=)** 조건에 한해 사용

### 동작 원리

1. **Build Phase**: 작은 테이블의 조인 키로 해시 테이블 생성
2. **Probe Phase**: 큰 테이블의 키를 해시 테이블에 lookup하여 일치 여부 확인

```sql
-- Build phase
hash_table := build_hash(small_table);
-- Probe phase
FOR row IN large_table LOOP
  IF row.key IN hash_table THEN
    RETURN joined_result;
  END IF;
END LOOP;
```

### 예시 쿼리

```sql
SELECT *
FROM sales s
JOIN products p ON s.product_id = p.id;
```

- 이때 PostgreSQL은 일반적으로 더 작은 products 테이블을 메모리에 해시로 만들어 사용

### 장점

* 정렬 불필요, 인덱스 불필요 → **데이터 전처리 비용이 낮음**
* 조인 키 기반 탐색이 **O(1)에 가까워 성능이 우수**
* 병렬화에 유리하고, 대용량 조인에서 강력함

### 단점

* 해시 테이블이 **RAM을 많이 사용**하며, 메모리가 부족하면 디스크를 사용할 수 있음 (성능 저하)
* 등호(=) 비교만 가능, 범위 조건(`>`, `<`, `BETWEEN`)에서는 사용 불가

### 언제 쓰일까?

* 조인 키에 인덱스가 없고 정렬되어 있지 않은 경우
* 중간 결과(서브쿼리, CTE, 임시 테이블 등)와의 조인

---

## 4. 세 가지 방식 요약 비교표

| 조인 방식       | 적합한 상황                   | 정렬 필요 | 인덱스 필요    | 메모리 의존 |
| ----------- | ------------------------ | ----- | --------- | ------ |
| Nested Loop | 한쪽 테이블이 작고 인덱스가 있을 때     | ❌     | ✅ (inner) | 낮음     |
| Merge Join  | 양쪽 정렬 또는 정렬 가능한 인덱스 존재 시 | ✅     | ❌/✅       | 낮음     |
| Hash Join   | 정렬, 인덱스 모두 없고 등호 조건일 때   | ❌     | ❌         | 높음     |

---

## 실행 계획 확인 방법 (EXPLAIN ANALYZE)

PostgreSQL에서 실제로 어떤 조인 전략이 선택되었는지 확인하려면 아래 명령어를 사용

```sql
EXPLAIN ANALYZE
SELECT *
FROM orders o
JOIN customers c ON o.customer_id = c.id;
```

- 이 명령을 통해 PostgreSQL의 실행 계획을 확인하고, 각 단계별 수행 시간과 조인 방식(Nested Loop, Merge Join, Hash Join 등)을 직접 분석
