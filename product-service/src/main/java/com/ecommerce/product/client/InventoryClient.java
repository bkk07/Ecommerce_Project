package com.ecommerce.product.client;

import com.ecommerce.product.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 1. Bind the FeignClientConfig here so the interceptor runs for this client
@FeignClient(name = "INVENTORY-SERVICE", configuration = FeignClientConfig.class)
public interface InventoryClient {

    // 2. Define the endpoint you are calling in Service B
    @GetMapping("/api/v1/inventory/{skuCode}")
    Boolean isInStock(@PathVariable("skuCode") String skuCode);

}