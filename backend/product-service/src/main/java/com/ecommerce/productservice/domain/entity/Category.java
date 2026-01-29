package com.ecommerce.productservice.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories", indexes = {
        // Index parent_id for fast tree traversals
        @Index(name = "idx_category_parent", columnList = "parent_id")
})
@Getter @Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // "Path Enumeration" Pattern for optimization
    // Example: "1/5/12" -> Allows searching entire subtrees with one SQL query:
    // WHERE path LIKE '1/5/%'
    private String path;

    // --- Recursive Relationship ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> subCategories = new ArrayList<>();

    // --- Relationship to Products ---

    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();

    // --- Helper Methods ---

    // Helps maintain the bidirectional link when adding a child
    public void addSubCategory(Category child) {
        subCategories.add(child);
        child.setParent(this);
    }
}