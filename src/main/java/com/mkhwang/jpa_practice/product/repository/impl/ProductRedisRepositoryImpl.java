package com.mkhwang.jpa_practice.product.repository.impl;

import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;
import com.mkhwang.jpa_practice.product.repository.ProductRedisRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRepository;
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
    return redisTemplate.opsForValue().multiGet(productIds.stream().map(String::valueOf).toList());
  }
}
