package com.mkhwang.jpa_practice.product.repository;

import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;

import java.util.List;

public interface ProductRedisRepository {

  List<ProductStock> findByIdIn(List<Long> productIds);

  void save(ProductStock productStock);

}
