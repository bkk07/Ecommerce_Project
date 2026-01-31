package com.ecommerce.cartservice.client;

import com.ecommerce.feigndtos.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(name = "search-service")
public interface SearchClient {
    @GetMapping("/api/v1/search/sku/{skuCode}")
    ProductResponse getProductBySku(@PathVariable("skuCode") String skuCode);
}