package com.mkhwang.jpa_practice.product.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Document(indexName = "product")
public class ProductDocument {
  @Id
  private Long id;
  @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")
  private String name;
  @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori")
  private String description;
}
