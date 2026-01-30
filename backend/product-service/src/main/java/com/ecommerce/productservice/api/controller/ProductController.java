package com.ecommerce.productservice.api.controller;

import com.ecommerce.product.ProductValidationItem;
import com.ecommerce.product.ProductValidationResponse;
import com.ecommerce.productservice.api.dto.ProductRequest;
import com.ecommerce.productservice.api.dto.ProductResponse;
import com.ecommerce.productservice.api.dto.ProductSkuResponse;
import com.ecommerce.productservice.domain.service.ImageService;
import com.ecommerce.productservice.domain.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ImageService imageService;

    // 1. Upload Image (Admin Only)
    @PostMapping("/images/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadImage(file));
    }

    // 2. Create Product (Admin Only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    // 3. Get Product by SKU (Public)
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductSkuResponse> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    // 4. Validate Products
    @PostMapping("/validate")
    public ProductValidationResponse validateProducts(@RequestBody List<ProductValidationItem> items) {
        return productService.validateProducts(items);
    }
}
