package com.mkhwang.jpa_practice.product.controller;

import com.mkhwang.jpa_practice.product.domain.Product;
import com.mkhwang.jpa_practice.product.service.ProductDocumentService;
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
}
