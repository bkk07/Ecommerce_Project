package com.ecommerce.searchservice.controller;

import com.ecommerce.searchservice.model.ProductDocument;
import com.ecommerce.searchservice.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // GET http://localhost:8083/api/v1/search?keyword=iphone
    @GetMapping
    public List<ProductDocument> searchProducts(@RequestParam String keyword) {
        return searchService.search(keyword);
    }
}