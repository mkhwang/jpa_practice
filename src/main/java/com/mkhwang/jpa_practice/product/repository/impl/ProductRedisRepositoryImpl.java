package com.mkhwang.jpa_practice.product.repository.impl;

import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;
import com.mkhwang.jpa_practice.product.repository.ProductRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductRedisRepositoryImpl implements ProductRedisRepository {
  private final RedisTemplate<String, ProductStock> redisTemplate;


  @Override
  public List<ProductStock> findByIdIn(List<Long> productIds) {
    Thread current = Thread.currentThread();
    System.out.printf("[1] Current thread: [%s] ID: %d, State: %s%n",
            current.getName(),
            current.getId(),
            current.getState());
    return redisTemplate.opsForValue().multiGet(productIds.stream().map(p -> "productStock:" + p.toString()).toList());
  }

  public void save(ProductStock productStock) {
    redisTemplate.opsForValue().set("productStock:" + String.valueOf(productStock.getId()), productStock);
  }
}
