package com.mkhwang.jpa_practice.product.domain.dto;


import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "product")
public class ProductMongo {
  private Long productId;
  private String name;
  private String description;
}
