package com.ecommerce.product;

import java.math.BigDecimal;

public class ProductValidationItem {

    private String skuCode;
    private BigDecimal price;

    public ProductValidationItem(){}

    public ProductValidationItem(String skuCode, BigDecimal price) {
        this.skuCode = skuCode;
        this.price = price;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}

