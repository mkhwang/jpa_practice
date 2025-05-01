# QueryDsl Expressions

## 0. Why?
- QueryDsl을 이용해서 sum(case when ...) 같은 SQL을 작성할 때 계속 에러가 발생했음
- 내가 원했던 쿼리 
  - ```
    select id,
         sum(case when rating = 1 then 1 else 0 end) as one_star,
         sum(case when rating = 2 then 1 else 0 end) as two_star,
         sum(case when rating = 3 then 1 else 0 end) as three_star,
         sum(case when rating = 4 then 1 else 0 end) as four_star,
         sum(case when rating = 5 then 1 else 0 end) as five_star
    from reviews group by id;
    ```
- 최초 QueryDsl
  - ```
    jpaQueryFactory
            .select(qReview.id,
                    qReview.rating.when(1).then(1).otherwise(0).count().as("one_star"),
                    qReview.rating.when(2).then(1).otherwise(0).count().as("two_star"),
                    qReview.rating.when(3).then(1).otherwise(0).count().as("three_star"),
                    qReview.rating.when(4).then(1).otherwise(0).count().as("four_star"),
                    qReview.rating.when(5).then(1).otherwise(0).count().as("five_star"))
            .from(qReview)
            .groupBy(qReview.id)
            .fetch();
    ```
- 계속 실패...
- 그래서 검색한 결과 Expressions를 사용해야 한다는 것을 알게 됨

## 1. Expressions
- SQL 표현식(Expression)을 직접 정의할 수 있게 해주는 유틸리티 클래스
- Q객체를 사용하는 일반적인 DSL 문법보다 더 유연하고, 커스텀 SQL 표현을 작성할 때 자주 사용

## 2. 주요 method
### 2-.1 Expressions.constant
- 상수 표현식

### 2-2. Expressions.as
- Alias 표현식

### 2-3. Expressions.stringTemplate
- SQL 템플릿은 SQL 쿼리의 일부를 동적으로 생성할 수 있는 방법
- ```
  query.select(Expressions.stringTemplate("replace({0}, {1}, {2})", qProduct.name, "상품", "테스트"));
  ```
- SELECT replace(name, '상품', '테스트')
- stringTemplate()은 {0}, {1}로 파라미터 바인딩됨
- JPA 표준이 아닌 DB 고유 함수 사용 시 function('...') 형태로 써야 함
  - `Expressions.stringTemplate("function('func_name', {0})", qUser.email).eq("test@example.com")`

### 2-4. Expressions.numberTemplate
- ```
  NumberTemplate<Long> sumCase = Expressions.numberTemplate(
  Long.class,
  "SUM(CASE WHEN {0} = 'ACTIVE' THEN 1 ELSE 0 END)",
  qUser.status
  );
  ```
- SELECT SUM(CASE WHEN user.status = 'ACTIVE' THEN 1 ELSE 0 END)

### 2-5. path() 동적 경로 생성
- ```
  Path<String> dynamicPath = Expressions.path(String.class, "userName");
  query.select(dynamicPath).from(qUser).fetch();
  ```
  
## 3. 참고
- 당연히 where 절에서도 사용 가능
- ```
  NumberTemplate<Integer> isAdult = Expressions.numberTemplate(
  Integer.class,
  "CASE WHEN {0} >= 20 THEN 1 ELSE 0 END",
  qUser.age
  );
  
  queryFactory.selectFrom(qUser)
  .where(isAdult.eq(1))
  .fetch();
  ```