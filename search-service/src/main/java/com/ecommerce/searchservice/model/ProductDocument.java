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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// "indexName" is like the Table Name in MySQL
@Document(indexName = "product")
public class ProductDocument {

    @Id
    private String id; // This will store the Product ID
    @Field(type = FieldType.Text, name = "name")
    private String name;
    @Field(type = FieldType.Text, name = "description")
    private String description;
    @Field(type = FieldType.Keyword, name = "skuCode")
    private String skuCode;
    @Field(type = FieldType.Double, name = "price")
    private BigDecimal price;
    @Field(type = FieldType.Boolean, name = "isInStock")
    private boolean isInStock;
}