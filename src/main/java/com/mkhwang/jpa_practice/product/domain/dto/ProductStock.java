package com.mkhwang.jpa_practice.product.domain.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@RedisHash(value = "productStock")
public class ProductStock implements Serializable {
  @Id
  private Long id;
  private Long stockCnt;

  public ProductStock(Long id, Long stockCnt) {
    this.id = id;
    this.stockCnt = stockCnt;
  }
}
