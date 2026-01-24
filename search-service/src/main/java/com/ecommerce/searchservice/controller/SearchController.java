package com.ecommerce.searchservice.controller;

import com.ecommerce.feigndtos.ProductResponse;
import com.ecommerce.searchservice.dto.SearchResponse;
import com.ecommerce.searchservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // GET http://localhost:8083/api/v1/search?keyword=iphone&category=Electronics&minPrice=100&maxPrice=1000&page=0&size=10
    @GetMapping
    public ResponseEntity<SearchResponse> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        
        return ResponseEntity.ok(searchService.search(keyword, category, brand, minPrice, maxPrice, pageable));
    }

    // GET http://localhost:8083/api/v1/search/sku/skuCode
    @GetMapping("/sku/{skuCode}")
    public ResponseEntity<ProductResponse> getProductBySkuCode(@PathVariable String skuCode) {
        ProductResponse productResponse = searchService.getProductBySkuCode(skuCode);
        if (productResponse == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productResponse);
    }
}
