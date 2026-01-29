package com.ecommerce.productservice.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_image_variant", columnList = "variant_id")
})
@Getter @Setter
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the ProductVariant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

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