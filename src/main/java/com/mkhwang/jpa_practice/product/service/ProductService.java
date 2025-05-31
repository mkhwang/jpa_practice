package com.mkhwang.jpa_practice.product.service;

import com.mkhwang.jpa_practice.product.domain.Product;
import com.mkhwang.jpa_practice.product.domain.QProduct;
import com.mkhwang.jpa_practice.product.domain.dto.ProductGroupBy;
import com.mkhwang.jpa_practice.product.repository.ProductRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository productRepository;
  private final JPAQueryFactory jpaQueryFactory;
  private final QProduct qProduct = QProduct.product;

  public List<Product> getAllProducts() {

    this.replaceTestProduct();
    List<ProductGroupBy> fetch = jpaQueryFactory.select(
                    Projections.constructor(
                            ProductGroupBy.class,
                            qProduct.name,
                            qProduct.id.count()
                    )).from(qProduct).groupBy(qProduct.name)
            .orderBy(OrderByNull.DEFAULT)
            .fetch();
    System.out.println(fetch);
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

  public void replaceTestProduct() {
    Product product = jpaQueryFactory.select(
            Projections.constructor(
                    Product.class,
                    qProduct.id,
                    Expressions.stringTemplate("replace({0}, {1}, {2})", qProduct.name, "상품", "테스트"),
                    qProduct.name
            )
    ).from(qProduct).where(qProduct.name.eq("상품1")).fetchOne();
    assert product != null;
    System.out.println(product.getName().toString());
  }
}
