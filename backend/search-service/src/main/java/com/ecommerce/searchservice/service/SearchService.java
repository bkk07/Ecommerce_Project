package com.ecommerce.searchservice.service;

import com.ecommerce.feigndtos.ProductResponse;
import com.ecommerce.searchservice.dto.SearchResponse;
import com.ecommerce.searchservice.model.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchResponse search(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    /* ---------- Full text ---------- */
                    if (keyword != null && !keyword.isBlank()) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .fields("name", "description")
                                .query(keyword)
                                .fuzziness("AUTO")));
                    }

                    /* ---------- Filters ---------- */
                    if (category != null && !category.isBlank()) {
                        b.filter(f -> f.term(t -> t
                                .field("category")
                                .value(category)));
                    }

                    if (brand != null && !brand.isBlank()) {
                        b.filter(f -> f.term(t -> t
                                .field("brand")
                                .value(brand)));
                    }

                    if (minPrice != null || maxPrice != null) {
                        b.filter(f -> f.range(r -> r.number(n -> {
                            n.field("price");
                            if (minPrice != null) n.gte(minPrice.doubleValue());
                            if (maxPrice != null) n.lte(maxPrice.doubleValue());
                            return n;
                        })));
                    }

                    return b;
                }))

                /* ---------- Aggregations ---------- */
                .withAggregation("categories", Aggregation.of(a -> a
                        .terms(t -> t.field("category"))
                ))

                .withAggregation("brands", Aggregation.of(a -> a
                        .terms(t -> t.field("brand"))
                ))

                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(query, ProductDocument.class);

        /* ---------- Products ---------- */
        List<ProductResponse> products = searchHits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());

        Page<ProductResponse> page =
                new PageImpl<>(products, pageable, searchHits.getTotalHits());

        /* ---------- Facets ---------- */
        Map<String, Long> categoryFacets = extractTermsFacet(searchHits, "categories");
        Map<String, Long> brandFacets = extractTermsFacet(searchHits, "brands");

        return SearchResponse.builder()
                .products(page)
                .categoryFacets(categoryFacets)
                .brandFacets(brandFacets)
                .build();
    }

    /* ================= FACETS (SPRING SAFE) ================= */

    private Map<String, Long> extractTermsFacet(
            SearchHits<ProductDocument> searchHits,
            String name
    ) {

        Map<String, Long> facets = new HashMap<>();

        if (searchHits.getAggregations() == null) return facets;

        if (searchHits.getAggregations() instanceof ElasticsearchAggregations aggregations) {
            ElasticsearchAggregation aggregation = aggregations.get(name);

            if (aggregation == null) return facets;

            Aggregate aggregate = aggregation.aggregation().getAggregate();

            if (aggregate.isSterms()) {
                StringTermsAggregate termsAggregate = aggregate.sterms();
                for (StringTermsBucket bucket : termsAggregate.buckets().array()) {
                    facets.put(bucket.key().stringValue(), bucket.docCount());
                }
            }
        }

        return facets;
    }

    /* ================= GET BY SKU ================= */

    public ProductResponse getProductBySkuCode(String skuCode) {
        if (skuCode == null || skuCode.isBlank()) return null;

        ProductDocument doc =
                elasticsearchOperations.get(skuCode, ProductDocument.class);

        return doc == null ? null : mapToProductResponse(doc);
    }

    /* ================= DELETE ALL ================= */

    public void deleteAllData() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        indexOps.delete();
        indexOps.create();
        indexOps.putMapping(ProductDocument.class);
    }

    /* ================= MAPPER ================= */

    private ProductResponse mapToProductResponse(ProductDocument doc) {

        ProductResponse response = new ProductResponse();
        response.setId(doc.getProductId());
        response.setName(doc.getName());
        response.setDescription(doc.getDescription());
        response.setPrice(doc.getPrice());
        response.setSkuCode(doc.getSkuCode());

        return response;
    }
}
