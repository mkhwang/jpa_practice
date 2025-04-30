package com.mkhwang.jpa_practice.product.service;

import com.mkhwang.jpa_practice.product.domain.Product;
import com.mkhwang.jpa_practice.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class ProductServiceTest {

  @Autowired
  ProductRepository productRepository;

  @DisplayName("상품 생성")
  @Test
  void test_create_product() {
    Product product = Product.of("상품1", "상품1 설명");
    productRepository.save(product);
  }

  @DisplayName("상품 생성")
  @Test
  void test_update_product() {
    Product product = productRepository.findByName("상품1").orElseThrow(RuntimeException::new);
    product.setDescription("상품1 수정 설명");
    productRepository.save(product);
  }

  @DisplayName("상품 목록 조회")
  @Test
  void testGetAllProducts() {
    productRepository.findByName("상품1").ifPresent(product -> {
      assertEquals("상품1", product.getName());
      assertEquals("상품1 설명", product.getDescription());
    });
  }
}