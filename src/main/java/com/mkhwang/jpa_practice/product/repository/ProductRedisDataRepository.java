package com.mkhwang.jpa_practice.product.repository;

import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRedisDataRepository extends CrudRepository<ProductStock, Long> {

  List<ProductStock> findByIdIn(List<Long> productIds);

}
