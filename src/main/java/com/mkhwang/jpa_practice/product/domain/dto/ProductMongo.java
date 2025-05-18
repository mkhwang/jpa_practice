package com.mkhwang.jpa_practice.product.domain.dto;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "product")
public class ProductMongo {
  @Id
  private String id;
  private Long productId;
  private String name;
  private String description;

  public ProductMongo(Long productId, String name, String description) {
    this.productId = productId;
    this.name = name;
    this.description = description;
  }
}
