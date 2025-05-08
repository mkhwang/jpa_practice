package com.mkhwang.jpa_practice.product.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.mkhwang.jpa_practice.product.domain.dto.ProductDocument;
import com.mkhwang.jpa_practice.product.repository.ProductDocumentRepository;
import com.mkhwang.jpa_practice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ProductDocumentService {
  private final ProductRepository productRepository;
  private final ProductDocumentRepository productDocumentRepository;
  private final ElasticsearchOperations elasticsearchOperations;

  @Transactional(readOnly = true)
  public void saveAllProducts() {
    List<ProductDocument> list = productRepository.findAll().stream().map(
            product -> {
              ProductDocument productDocument = new ProductDocument();
              productDocument.setId(product.getId());
              productDocument.setName(product.getName());
              productDocument.setDescription(product.getDescription());
              return productDocument;
            }
    ).toList();
    productDocumentRepository.saveAll(list);
  }


  public void searchProducts(String keyword) {
    Query nastedQuery = Query.of(q -> q
            .nested(n -> n
                    .path("tags")
                    .query(nq -> nq
                            .bool(b -> b
                                    .must(List.of(
                                            Query.of(q1 -> q1.term(t -> t.field("tags.key").value("color"))),
                                            Query.of(q2 -> q2.term(t -> t.field("tags.value").value("blue")))
                                    ))
                            )
                    )
            )
    );


    NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.functionScore(fs -> fs
                    .query(inner -> inner.bool(b -> b
                            .should(s -> s.match(m -> m.field("id").query(keyword)))
                            .should(s -> s.match(m -> m.field("name").query(keyword)))
                            .should(s -> s.match(m -> m.field("description").query(keyword)))
//                            .should(nastedQuery)
                    ))
            ))
            .build();

    SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
    List<ProductDocument> list = hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .toList();
    for (ProductDocument product : list) {
      System.out.println("Product ID: " + product.getId());
      System.out.println("Product Name: " + product.getName());
      System.out.println("Product Description: " + product.getDescription());
      System.out.println("------------------------------");
    }
  }

}
