package com.ecommerce.searchservice.service;

import com.ecommerce.searchservice.model.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public List<ProductDocument> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            // Sanitize input to prevent JSON injection and remove quotes
            String sanitized = keyword.replace("\"", "")
                                      .replace("\\", "")
                                      .replace("\n", " ")
                                      .replace("\r", " ")
                                      .trim();

            if (sanitized.isEmpty()) {
                return List.of();
            }

            // Use StringQuery with multi_match for a robust full-text search
            // This searches both 'name' and 'description' fields
            String queryString = String.format(
                    "{\"multi_match\": {\"query\": \"%s\", \"fields\": [\"name\", \"description\"]}}",
                    sanitized
            );

            Query query = new StringQuery(queryString);

            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);

            return searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching for products with keyword: {}", keyword, e);
            throw e;
        }
    }
}