# Redis ZSet

## 0. Why?
- 과거 Redis Sorted Set을 사용하여 랭킹 기능을 캐싱을 통해 구현했다는 블로그가 생각이 났다
- Redis Sorted Set을 사용하여 랭킹 기능을 구현하는 방법을 정리하기 위해 작성했다

## 1. Sorted Set(ZSet) 이란?
- **중복 없는 값(value)**을 저장하면서, 각 값에 대해 **정렬 기준이 되는 score(실수)**를 함께 저장하는 자료구조
- 일반적인 Set처럼 고유값을 보장하면서, 추가로 score 기준 자동 정렬 기능까지 제공
- 정렬렬 기준 : score 값 기준으로 자동 정렬 (오름차순)
- 주요 용도: 랭킹 시스템, 우선순위 큐, 시간순 정렬, 탑N 집계 등
- 내부 구조: Skip List + HashMap 조합으로 구현됨

### 1-1. 주요 명령어

| 명령어 | 설명 |
|--------|------|
| ZADD key score member [score member ...] | 값 추가/갱신 |
| ZRANGE key start stop [WITHSCORES] | 오름차순 조회 |
| ZREVRANGE key start stop [WITHSCORES] | 내림차순 조회 |
| ZINCRBY key increment member | 점수 증가 |
| ZSCORE key member | 특정 멤버의 score 조회 |
| ZRANK key member / ZREVRANK key member | 특정 멤버의 순위 조회 |
| ZREM key member [member ...] | 멤버 삭제 |
| ZCOUNT key min max | 특정 score 범위 내 항목 개수 |
| ZRANGEBYSCORE key min max [LIMIT offset count] | 점수 조건 조회 |

### 1-2. 주의사항
- member 값은 고유해야 함 → 같은 값을 다시 넣으면 score가 덮어씀
- score는 double 타입 → 0.0, 1.23 등 가능
- 하나의 key(ZSet)에 최대 약 43억 개의 요소 저장 가능 (이론상)

## 2. Spring 에서 사용

### 2-1. RedisTemplate
```java
private final RedisTemplate<String, String> redisTemplate;
```
- RedisTemplate<String, String>은 ZSet의 member를 사람이 읽을 수 있는 문자열로 직렬화하여 저장
- 불필요한 직렬화 비용과 호환성 문제를 회피하기 위해 사용하는 최적의 선택

### 2-2. ZSetOperations
- 단일 ADD
- ```java
  redisTemplate.opsForZSet().add(key, value, score);
  ```
- 복수 ADD
- ```java
  Set<ZSetOperations.TypedTuple<String>> tuples = all.stream().map(
            product -> {
              return new DefaultTypedTuple<>(product.getId().toString(), product.getId().doubleValue() * 10);
            }
    ).collect(Collectors.toSet());
  redisTemplate.opsForZSet().add(this.rankKey, tuples);
  ```
- 조회
- ```java
  Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().range(key, 0, -1);
  Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
  ```
  

## 3. 주의사항

### 3-1. RedisConfig
- 보통 key-value로 사용하기 때문에, RedisConfig에서 `GenericJackson2JsonRedisSerializer` 로 설정된 `RedisTemplate<String, Object> redisTemplate` bean 을 등록하여 사용
- `private final RedisTemplate<String, String> redisTemplate;` 이라고 선언했지만, 실제로는 Spring이 타입이 일치하지 않아도 Bean 주입을 시도하며,  등록된 RedisTemplate<String, Object> Bean을 여기에 억지로 넣어준 것
- 즉, 이건 타입이 다른데도 Spring이 강제로 넣어준 것 (raw type 호환)
- JVM 레벨에서는 RedisTemplate<?, ?>은 generic이 runtime에 지워지기 때문에 컴파일은 통과
- why?
  - Java의 제네릭은 type erasure 방식 → 런타임에는 RedisTemplate<String, Object>나 RedisTemplate<String, String>이나 동일하게 동작
  - Spring의 @Autowired/@RequiredArgsConstructor는 Bean 타입만 맞으면 주입을 시도함
  - 따라서, 등록된 단 하나의 RedisTemplate<String, Object>를 두 곳에 모두 주입한 것
- 그런데 왜 문제가 안 터졌을까? 
  - 이유 1: ZSetOperations<TypedTuple<String>>는 내부적으로 String 직렬화 
  - new DefaultTypedTuple<>("123", 950.0)
  - valueSerializer가 GenericJackson2JsonRedisSerializer 임에도 불구하고
  - String은 그대로 JSON 문자열 "123"으로 저장됨 (Redis에 "\"123\""으로 저장됨)
  - 즉, ZSet에 "123" → \"123\"으로 들어가도 ZREVRANGE로 읽고 다시 String으로 역직렬화되기 때문에, 문제 없이 보이는 것
- 하지만 문제는 “잠재적”이다
  - CLI에서 보면 ZSet member가 "\"123\""처럼 JSON 문자열로 감싸진 형태로 보일 것
  - 이후 ZREM, ZRANK, ZSCAN 등 명령 시 \"123\"으로 정확히 써야 작동
  - 다른 시스템(Python, Go)과 연동하거나 수동 조작 시 헷갈림과 버그 유발
- 결론 : 지금은 우연히 정상작동 중이다
- 작동 이유 : Java 제네릭 지워짐 + String도 JSON 직렬화 가능해서 눈에 띄는 오류 없음
- 위험 요소 : Redis에 member 값이 "\"123\"" 같은 JSON 문자열로 저장됨
- 방지책 : RedisTemplate<String, String>을 별도로 명확하게 등록해야 함
- 설계 추천 : stringRedisTemplate vs objectRedisTemplate을 분리하고, @Qualifier로 명시 주입

### 3-2. RedisConfig
```java
@Bean
public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory factory) {
  RedisTemplate<String, String> template = new RedisTemplate<>();
  template.setConnectionFactory(factory);
  template.setKeySerializer(new StringRedisSerializer());
  template.setValueSerializer(new StringRedisSerializer());
  template.afterPropertiesSet();
  return template;
}

@Bean
public RedisTemplate<String, Object> redisTemplate(...) { ... }
```
```java
@Service
@RequiredArgsConstructor
public class ProductRankService {
  @Qualifier("stringRedisTemplate")
  private final RedisTemplate<String, String> redisTemplate;

  @Qualifier("redisTemplate") // Object용
  private final RedisTemplate<String, Object> redisTemplate2;
}
```
