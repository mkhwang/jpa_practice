package com.mkhwang.jpa_practice.product.service;

import com.mkhwang.jpa_practice.product.domain.Product;
import com.mkhwang.jpa_practice.product.domain.dto.ProductMongo;
import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;
import com.mkhwang.jpa_practice.product.repository.ProductMongoRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRedisDataRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRedisRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductRankService {
  private final RedisTemplate <String, String> redisTemplate;
  private final RedisTemplate <String, Object> redisTemplate2;
  private final String rankKey= "productRank";
  private final ProductRepository productRepository;
  private final ProductRedisDataRepository productRedisDataRepository;
  private final ProductMongoRepository productMongoRepository;
  private final ProductRedisRepository productRedisRepository;

  public void setRank() {
    List<Product> all = productRepository.findAll();
    Set<ZSetOperations.TypedTuple<String>> tuples = all.stream().map(
            product -> {
              return new DefaultTypedTuple<>(product.getId().toString(), product.getId().doubleValue() * 10);
            }
    ).collect(Collectors.toSet());
    redisTemplate.opsForZSet().add(this.rankKey, tuples);
    Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(this.rankKey, 0, -1);
    typedTuples.stream().forEach(System.out::println);

    productRepository.findById(3L).ifPresent(
            product -> {
              redisTemplate2.opsForValue().set("product:3", product);
            }
    );

    Object o = redisTemplate2.opsForValue().get("product:3");
    System.out.println(o.toString());

    all.forEach(
            product -> {
              productRedisRepository.save(new ProductStock(product.getId(), product.getId()*10 ));
              productMongoRepository.save(new ProductMongo(product.getId(), product.getName(), product.getDescription()));
            }
    );
  }
}
