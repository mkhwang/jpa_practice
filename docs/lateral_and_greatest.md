# SQL LATERAL & GREATEST

## Why ? 
- 블로그를 보다가 쿼리를 봤는데, 이런 키워드가 있었나 해서 확인해봄 

## What is LATERAL ?

- LATERAL JOIN은 PostgreSQL, MySQL 8 이상, Oracle, SQL Server 등에서 지원되는 고급 SQL 기능
- 서브쿼리를 각 행마다 동적으로 실행할 수 있게 해주는 JOIN 방식

### JOIN vs LATERAL

| **구분**           | **일반 JOIN** | **LATERAL JOIN** |
| ---------------- | ----------- | ---------------- |
| 서브쿼리에서 외부 테이블 참조 | ❌ 불가능       | ✅ 가능             |
| 서브쿼리 1번만 실행      | ✅           | ❌ 각 행마다 실행       |


### When use ?
- 서브쿼리 안에서 외부 테이블(앞쪽 FROM에 있는 테이블)의 값을 **참조**하고 싶을 때
- **가장 최신 row, top N, row_number = 1** 조건을 각 행마다 다르게 처리할 때


#### examples


> 각 매장별로 **가장 최근 매출 1건**만 가져오고 싶을 때:

##### **일반 서브쿼리로는 한계**

```
-- 서브쿼리를 여러번 날려야함
SELECT s.*, 
       (select sa.? from sales sa where sa.store_id = s.id) as sales_1,
       (select sa.? from sales sa where sa.store_id = s.id) as sales_2,
       (select sa.? from sales sa where sa.store_id = s.id) as sales_3
FROM store s
```

##### **LATERAL JOIN 사용**

```
SELECT s.id, s.name, recent_sales.amount, recent_sales.date
FROM store s
JOIN LATERAL (
    SELECT *
    FROM sales sa
    WHERE sa.store_id = s.id
    ORDER BY sa.date DESC
    LIMIT 1
) AS recent_sales ON true;
```

> sales 서브쿼리가 **매장 s마다 1번씩 실행**되어, 해당 매장의 최신 매출 1건만 조회됨

---

#### **PostgreSQL에서의 특성**

- JOIN LATERAL 또는 그냥 , LATERAL (...)로도 가능
- ON true는 필수 (LATERAL은 조건 없이 항상 붙으므로)


### **사용 시 주의점**

|**항목**|**설명**|
|---|---|
|성능|각 행마다 서브쿼리 실행 → **성능 주의**, 반드시 인덱스 필요|
|DB 지원|PostgreSQL, MySQL 8+, Oracle, SQL Server 2012+|
|JOIN 타입|INNER JOIN LATERAL, LEFT JOIN LATERAL 모두 가능|

- row_number()나 rank() 쓰기 어려운 DB 버전에서 대체 가능
- Spring Data JPA 또는 QueryDSL에서는 NativeQuery로 사용해야 함


## What is GREATEST ? 

- SQL에서 GREATEST()는 여러 값 중에서 **가장 큰 값을 반환하는 함수**
- 쉽게 말해, **숫자든 날짜든 문자열이든 “제일 큰 놈 하나 골라줘”** 라고 할 때 사용

### How use ?

```
GREATEST(val1, val2, val3, ...)
```

- val1, val2…는 숫자, 날짜, 문자열 등 **동일한 타입**이어야 함
- NULL이 포함되면 기본적으로 **결과도 NULL**

### Example

#### **숫자 예제**

```
SELECT GREATEST(10, 5, 20); -- 결과: 20
```

#### **날짜 예제**

```
SELECT GREATEST('2024-01-01', '2025-06-01', '2023-12-31'); 
-- 결과: 2025-06-01
```

#### **문자열 예제**

```
SELECT GREATEST('apple', 'banana', 'cherry'); 
-- 결과: cherry (알파벳 순으로 가장 뒤)
```
#### **주의: NULL 처리**

```
SELECT GREATEST(10, NULL, 20); -- 결과: NULL
```

#### **→ NULL 무시하고 싶다면?**

- PostgreSQL이라면 COALESCE() 조합 사용:

```
SELECT GREATEST(COALESCE(val1, 0), COALESCE(val2, 0), COALESCE(val3, 0))
```
#### **조건 비교**

```
SELECT user_id, GREATEST(score_a, score_b, score_c) AS max_score
FROM user_scores;
```

---

### 반대 함수: LEAST()

- LEAST()는 가장 **작은 값** 반환
- 사용법은 동일

---

#### **사용 가능 DB**

|**DBMS**|**지원 여부**|
|---|---|
|PostgreSQL|✅ 지원|
|MySQL|✅ 지원|
|Oracle|✅ 지원|
|SQL Server|❌ (직접 CASE WHEN 등으로 구현해야 함)|

