package com.mkhwang.jpa_practice.product.service;

import com.mkhwang.jpa_practice.product.domain.Product;
import com.mkhwang.jpa_practice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository productRepository;

  public List<Product> getAllProducts() {
    return productRepository.findAll();
  }

  @Transactional
  public Product createProduct(Product product) {
    return productRepository.save(product);
  }

  @Transactional
  public void updateProduct() {
    Product product = productRepository.findByName("상품1").orElseThrow(RuntimeException::new);
    product.setDescription("상품1 수정 설명");
    productRepository.save(product);
  }
}
