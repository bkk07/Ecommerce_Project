package com.ecommerce.productservice.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_image_product", columnList = "product_id")
})
@Getter @Setter
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // The secure URL from Cloudinary/S3 (e.g., https://res.cloudinary.com/...)
    @Column(nullable = false, length = 500)
    private String url;

    // Optional: The Public ID allows you to delete or transform the image later via API
    private String publicId;

    // Is this the main image shown on the search page?
    private boolean isPrimary;

    // Allows Admin to drag-and-drop sort images in the gallery
    private int sortOrder;
}