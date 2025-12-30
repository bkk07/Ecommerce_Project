package com.ecommerce.cartservice.client;

import com.ecommerce.cartservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/v1/products/sku/{skuCode}")
    ProductResponse getProductBySku(@PathVariable("skuCode") String skuCode);
}