package com.mkhwang.jpa_practice.product.query;

import com.mkhwang.jpa_practice.product.domain.dto.ProductDocument;
import com.mkhwang.jpa_practice.product.domain.dto.ProductMongo;
import com.mkhwang.jpa_practice.product.domain.dto.ProductResponse;
import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;
import com.mkhwang.jpa_practice.product.repository.ProductDocumentRepository;
import com.mkhwang.jpa_practice.product.repository.ProductMongoRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRedisRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQueryService {
  private final ProductRedisRepository productRedisRepository;
  private final ProductDocumentRepository productDocumentRepository;
  private final ProductMongoRepository productMongoRepository;

  public List<ProductResponse> getProducts(String keyword) {
    List<ProductDocument> result = productDocumentRepository.findByName("keyword");

    List<Long> productIds = result.stream().map(ProductDocument::getId).toList();

    CompletableFuture<List<ProductMongo>> productMongoFuture = CompletableFuture.supplyAsync(
            () -> productMongoRepository.findByIdIn(productIds)
    );

    CompletableFuture<List<ProductStock>> productStockFuture = CompletableFuture.supplyAsync(
            () -> productRedisRepository.findByIdIn(productIds)
    );

    CompletableFuture.allOf(productMongoFuture, productStockFuture).join();
    List<ProductMongo> productMongoList = productMongoFuture.join();
    List<ProductStock> productStockList = productStockFuture.join();


    return null;
  }

}
