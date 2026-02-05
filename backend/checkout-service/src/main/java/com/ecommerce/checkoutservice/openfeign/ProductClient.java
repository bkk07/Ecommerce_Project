package com.ecommerce.checkoutservice.openfeign;

import com.ecommerce.product.ProductValidationItem;
import com.ecommerce.product.ProductValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {
    @PostMapping("/api/v1/products/validate")
    ProductValidationResponse validateProducts(@RequestBody List<ProductValidationItem> items);
}
