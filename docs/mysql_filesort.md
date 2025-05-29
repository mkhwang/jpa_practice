# Mysql FileSort

## 0. Why FileSort ?
- youtube에서 querydls [관련 영상](https://youtu.be/zMAX7g6rO_Y?si=h3yoo0yeGJPOAHSB)을 보다가 filesort 회피하는 방법에 대한 정보를 얻었다.
- filesort가 뭔데 피하기까지 해야할까 ?

## 1. What is FileSort ?
- MySQL에서 쿼리 결과를 정렬할 때 사용하는 내부 메커니즘
- 이름에 “file”이 들어있지만, 반드시 디스크에 정렬 데이터를 쓴다는 뜻은 아니고, 메모리로 처리하지 못할 경우 디스크에 임시 파일을 써서 정렬한다는 의미

## 2. When does FileSort happen ?
1.	ORDER BY 절에 명시된 컬럼이 인덱스에 포함되어 있지 않거나, 인덱스를 사용할 수 없는 경우
2.	인덱스를 사용할 수 있어도 정렬 조건과 인덱스가 일치하지 않는 경우
3.	LIMIT이 함께 있어도 인덱스로 커버되지 않으면 filesort 발생

```sql
SELECT * FROM users ORDER BY name;
```
> - name 컬럼에 인덱스가 없다면 → filesort 발생
> - name 인덱스가 있어도 WHERE age = 30 ORDER BY name 같이 조건과 정렬 컬럼이 분리돼 있으면 → filesort 발생 가능

- MySQL 8.0 기준으로 두 가지 방법 중 하나를 사용
  - 1. One-Pass: 정렬 대상 데이터를 메모리에 다 올려서 정렬 (메모리 내 정렬)
  - 2. Two-Pass: 메모리를 초과하면 디스크 임시 파일에 데이터를 저장한 후 정렬 (디스크 사용)

- 이때 사용할 수 있는 메모리는 sort_buffer_size 시스템 변수에 의해 결정


## 3. So.. What's problem? 

- 성능 이슈?
  - filesort 자체가 무조건 나쁜 건 아니다
  - 하지만 대량 데이터에서 Using filesort가 자주 발생하면 성능 저하 가능성이 크기 때문에 튜닝 대상
  - **100건 미만은 WAS에서 정렬하자**

## 4. How to avoid FileSort ?
- ORDER BY에 사용되는 컬럼에 적절한 인덱스 생성
- WHERE, ORDER BY 순서와 인덱스 정의 순서를 일치
- 가능하다면 Covering Index 활용 (SELECT 대상까지 인덱스에 포함)
- `order by null`

## 5. What is order by null?
- GROUP BY를 사용할 때 MySQL은 기본적으로 결과를 정렬된 상태로 반환하려고 시도
- 하지만 여러분이 GROUP BY의 결과 정렬이 필요하지 않다면, MySQL에게 정렬을 하지 말라고 명시적으로 지시 = `ORDER BY NULL` 사용
- 왜 이런 일이 발생하나? 
  - GROUP BY는 내부적으로 GROUP → 정렬 → 집계 흐름으로 처리 
  - MySQL은 결과를 정렬해서 출력하는 걸 기본 동작으로 보기 때문에 정렬을 위한 filesort가 기본적으로 발생 
  - ORDER BY NULL을 쓰면 → “정렬하지 말고 그냥 집계된 결과만 출력해”라고 지시하는 셈이어서 → filesort 생략 가능

## 6. How in JPA ?

[https://jojoldu.tistory.com/477](https://jojoldu.tistory.com/477)

```java
List<ProductGroupBy> fetch = jpaQueryFactory.select(
            Projections.constructor(
                    ProductGroupBy.class,
                    qProduct.name,
                    qProduct.id.count()
            )).from(qProduct).groupBy(qProduct.name)
            .orderBy(OrderByNull.DEFAULT)
            .fetch();
```

```java
public class OrderByNull extends OrderSpecifier {
  public static final OrderByNull DEFAULT = new OrderByNull();

  private OrderByNull() {
    super(Order.ASC, NullExpression.DEFAULT, NullHandling.Default);
  }
}
```

