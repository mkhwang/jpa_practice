package com.mkhwang.jpa_practice.product.controller;

import com.mkhwang.jpa_practice.product.domain.Product;
import com.mkhwang.jpa_practice.product.domain.dto.ProductResponse;
import com.mkhwang.jpa_practice.product.query.ProductQueryService;
import com.mkhwang.jpa_practice.product.service.ProductDocumentService;
import com.mkhwang.jpa_practice.product.service.ProductRankService;
import com.mkhwang.jpa_practice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;
  private final ProductDocumentService productDocumentService;
  private final ProductRankService productRankService;
  private final ProductQueryService productQueryService;


  @GetMapping("/api/products")
  public List<Product> getAllProducts() {
    return productService.getAllProducts();
  }

  @GetMapping("/api/products/update")
  public boolean updateProduct() {
    productService.updateProduct();
    return true;
  }

  @GetMapping("/api/products/search")
  public void searchProducts(@RequestParam String keyword) {
    productDocumentService.saveAllProducts();
    productDocumentService.searchProducts(keyword);
  }

  @GetMapping("/api/products/cqrs/search")
  public List<ProductResponse> queryProducts(@RequestParam String keyword) {
    return productQueryService.getProducts(keyword);
  }


  @GetMapping("/api/products/rank/set")
  public boolean setRank() {
    productRankService.setRank();
    return true;
  }
}
