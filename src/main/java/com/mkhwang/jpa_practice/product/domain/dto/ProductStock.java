package com.mkhwang.jpa_practice.product.domain.dto;

import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "productStock")
public class ProductStock {
  private Long id;
  private Long stockCnt;
}
