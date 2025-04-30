package com.mkhwang.jpa_practice.product.repository;

import com.mkhwang.jpa_practice.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Optional<Product> findByName(String name);
}
