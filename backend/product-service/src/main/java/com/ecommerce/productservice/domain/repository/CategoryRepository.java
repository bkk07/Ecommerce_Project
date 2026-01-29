package com.ecommerce.productservice.domain.repository;

import com.ecommerce.productservice.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
