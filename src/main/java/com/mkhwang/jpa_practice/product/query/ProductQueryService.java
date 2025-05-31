package com.mkhwang.jpa_practice.product.query;

import com.mkhwang.jpa_practice.product.domain.dto.ProductDocument;
import com.mkhwang.jpa_practice.product.domain.dto.ProductMongo;
import com.mkhwang.jpa_practice.product.domain.dto.ProductResponse;
import com.mkhwang.jpa_practice.product.domain.dto.ProductStock;
import com.mkhwang.jpa_practice.product.repository.ProductMongoRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRedisRepository;
import com.mkhwang.jpa_practice.product.repository.ThreadTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class ProductQueryService {
  private final ProductRedisRepository productRedisRepository;
  private final ProductMongoRepository productMongoRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final Executor taskExecutor;
  private final ThreadTestRepository threadTestRepository;

  public List<ProductResponse> getProducts(String keyword) {
    Thread current = Thread.currentThread();
    System.out.printf("[0] Current thread: [%s] ID: %d, State: %s%n",
            current.getName(),
            current.getId(),
            current.getState());

    NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.functionScore(fs -> fs
                    .query(inner -> inner.bool(b -> b
                            .should(s -> s.match(m -> m.field("id").query(keyword)))
                            .should(s -> s.match(m -> m.field("name").query(keyword)))
                            .should(s -> s.match(m -> m.field("description").query(keyword)))
                    ))
            ))
            .build();

    SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
    List<ProductDocument> result = hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .toList();

    List<Long> productIds = result.stream().map(ProductDocument::getId).toList();

    CompletableFuture<List<ProductMongo>> productMongoFuture = CompletableFuture.supplyAsync(
            () -> productMongoRepository.findByProductIdIn((productIds)), taskExecutor
    );

    CompletableFuture<List<ProductStock>> productStockFuture = CompletableFuture.supplyAsync(
            () -> productRedisRepository.findByIdIn(productIds), taskExecutor
    );

    CompletableFuture<Integer> threadTestFuture = CompletableFuture.supplyAsync(
            threadTestRepository::someWorkAndReturnInteger, taskExecutor
    );

    CompletableFuture.allOf(productMongoFuture, productStockFuture, threadTestFuture).join();
    List<ProductMongo> productMongoList = productMongoFuture.join();
    List<ProductStock> productStockList = productStockFuture.join();
    Integer someWorkAndReturnInteger = threadTestFuture.join();
    System.out.println("someWorkAndReturnInteger: " + someWorkAndReturnInteger);

    List<ProductResponse> list = productIds.stream().map(
            id -> {
              ProductMongo productMongo = productMongoList.stream()
                      .filter(product -> product.getProductId().equals(id))
                      .findFirst()
                      .orElse(null);
              ProductStock productStock = productStockList.stream()
                      .filter(stock -> stock.getId().equals(id))
                      .findFirst()
                      .orElse(null);

              assert productMongo != null;
              assert productStock != null;
              System.out.println(productStock.toString());
              return new ProductResponse(productMongo, productStock);
            }

    ).toList();


    return list;
  }

}
