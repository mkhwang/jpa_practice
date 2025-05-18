package com.mkhwang.jpa_practice.product.repository;

import com.mkhwang.jpa_practice.product.domain.dto.ProductMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface ProductMongoRepository extends MongoRepository<ProductMongo, String> {

  List<ProductMongo> findByProductIdIn(Collection<Long> productIds);
}
