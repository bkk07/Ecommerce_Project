package com.ecommerce.searchservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "product")
public class ProductDocument {

    @Id
    private String id; // This will store the SKU Code as unique identifier

    @Field(type = FieldType.Long, name = "productId")
    private Long productId;

    @Field(type = FieldType.Text, name = "name", fielddata = true)
    private String name;

    @Field(type = FieldType.Keyword, name = "nameKeyword")
    private String nameKeyword;

    @Field(type = FieldType.Text, name = "description")
    private String description;

    @Field(type = FieldType.Keyword, name = "skuCode")
    private String skuCode;

    @Field(type = FieldType.Double, name = "price")
    private BigDecimal price;

    @Field(type = FieldType.Boolean, name = "isInStock")
    private boolean isInStock;

    @Field(type = FieldType.Keyword, name = "categories")
    private List<String> categories;

    @Field(type = FieldType.Keyword, name = "brand")
    private String brand;

    @Field(type = FieldType.Keyword, name = "imageUrl")
    private String imageUrl;

    // Rating fields
    @Field(type = FieldType.Double, name = "averageRating")
    private Double averageRating;

    @Field(type = FieldType.Long, name = "totalRatings")
    private Long totalRatings;
}
