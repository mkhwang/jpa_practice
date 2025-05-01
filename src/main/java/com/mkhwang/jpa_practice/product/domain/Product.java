package com.mkhwang.jpa_practice.product.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity(name = "products")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Lob
  @Basic(fetch = FetchType.EAGER)
  private String description;

  public static Product of(String name, String description) {
    Product product = new Product();
    product.setName(name);
    product.setDescription(description);
    return product;
  }
}
