# ElasticSearch 배포 전략

## 0. Why?
- ElasticSearch 를 통한 검색 서비스가 어느배포 시점 이후로 변경되어야 하는 경우가 있다.
- 이를 위해 배포 전략을 고민해보았다.

## 1. 배포 전략
- 우선 기존에 Elasticsearch 를 이용한 조회를 index를 바로 검색하지 않고, alias 를 이용한 조회로 변경한다.

### 1-1. alias 생성
- ```http request
    PUT /product_v1
  
    {
      "aliases": {
        "product": {}
      }
    }
  ```


### 1-2. alias 적용 
- ```http request
     POST /_aliases

     {
       "actions": [
         {
           "add": {
             "index": "product_v1",
             "alias": "product"
           }
         }
       ]
     }
  ```
- product index에 product_v1 alias를 추가한다.

### 1-3. 신규 index 생성

- ```http request
   PUT /product_v2

   {
     "aliases": {
       "product": {}
     }
   }
  ```
- 신규 index를 생성하고, alias를 추가한다.

### 1-4. Alias 교체 (zero-downtime 배포 방식)
- 
- ```http request
  POST /_aliases
  
  {
    "actions": [
      { "remove": { "index": "product_v1", "alias": "product" } },
      { "add":    { "index": "product_v2", "alias": "product" } }
    ]
  }
  ```

### 1-5. 쓰기전용 alias (write index)

- ```http request
  POST /_aliases
  
  {
    "actions": [
      { "add": { "index": "product_v2", "alias": "product", "is_write_index": true } }
    ]
  }
  ```
  
- is_write_index: true를 붙이면 product alias로 색인 요청을 보내면 product_v2로 들어감.
- 검색은 product alias를 통해 연결된 모든 인덱스에서 수행됨.

## 2. 왜 is_write_index 를 설정해야할까 ? 

- 하나의 alias로 여러 인덱스를 읽되, 특정 인덱스에만 쓰기(write)를 하도록 지정


#### 서비스 중단 없이 인덱스 교체 (Zero-Downtime Reindexing)

#### 2-1. 구조

|인덱스 이름|alias| 쓰기 권한                 |
|---|---|---------------------------|
|product_v1|product| X                         |
|product_v2|product| O (is_write_index = true) |

#### 2-2. 검색

```
GET /product/_search
```

→ product_v1, product_v2 모두 검색 대상


#### 2-3. 색인

```
POST /product/_doc
```

→ 오직 product_v2에만 문서가 저장됨

---

## 3 왜 중요할까?

### 3-1. 무중단 인덱스 교체 가능

- 새로운 인덱스를 만든 후 is_write_index: true로 alias를 지정
- 이전 인덱스는 읽기용으로 계속 유지
- 나중에 삭제하거나 보관

### 3-2. 색인 장애 예방

- 색인은 하나의 인덱스로만 들어가므로 충돌 방지
- 검색은 여러 인덱스에서 가능하므로 데이터 이관 중에도 서비스 유지


### 3-3.리인덱스 후 트래픽 전환 가능

- 색인을 멈추지 않고 새 인덱스로 점진적 이관
- /_aliases 명령으로 write alias만 바꾸면 끝


-----


## ✅ 요약

|이유|설명|
|---|---|
|💡 검색과 색인을 분리|읽기는 여러 인덱스, 쓰기는 하나로 제한|
|🚫 색인 충돌 방지|복수 인덱스에 동시에 색인되는 오류 방지|
|🔁 무중단 롤링|인덱스 교체 시 서비스 다운 없이 운영 가능|
|📊 검색 히스토리 보존|예전 인덱스는 그대로 두고, 검색에만 사용|
