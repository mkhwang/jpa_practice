package com.mkhwang.jpa_practice.product.domain.dto;


import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProductGroupBy {
  private String name;
  private Long count;
}
