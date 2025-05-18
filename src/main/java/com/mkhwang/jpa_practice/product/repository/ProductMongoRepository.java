package com.mkhwang.jpa_practice.product.repository;

import com.mkhwang.jpa_practice.product.domain.dto.ProductMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductMongoRepository extends MongoRepository<ProductMongo, Long> {

  List<ProductMongo> findByIdIn(List<Long> productIds);
}
