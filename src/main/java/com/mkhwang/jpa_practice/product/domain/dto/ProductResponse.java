package com.mkhwang.jpa_practice.product.domain.dto;

import lombok.Data;

@Data
public class ProductResponse {
  private Long id;

  private String name;

  private Long stock;

  public ProductResponse(Long id, String name, Long stock) {
    this.id = id;
    this.name = name;
    this.stock = stock;
  }

  public ProductResponse(ProductMongo productMongo, ProductStock productStock) {
    this.id = productMongo.getProductId();
    this.name = productMongo.getName();
    this.stock = productStock.getStockCnt();
  }
}
