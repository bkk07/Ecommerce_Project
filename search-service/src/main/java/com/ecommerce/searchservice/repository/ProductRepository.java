package com.ecommerce.searchservice.repository;

import com.ecommerce.searchservice.model.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, String> {

    // Custom query to find by SKU (since ID is the ProductId)
    Optional<ProductDocument> findBySkuCode(String skuCode);
}