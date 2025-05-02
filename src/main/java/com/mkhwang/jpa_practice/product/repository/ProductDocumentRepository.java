package com.mkhwang.jpa_practice.product.repository;

import com.mkhwang.jpa_practice.product.domain.dto.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, Long> {
}
