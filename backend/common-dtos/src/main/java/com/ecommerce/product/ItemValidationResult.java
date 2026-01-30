package com.ecommerce.product;

import java.math.BigDecimal;

public class ItemValidationResult {

    private String skuCode;
    private boolean valid;
    private ProductValidationFailureReason reason;
    private BigDecimal currentPrice; // only for PRICE_MISMATCH

    public ItemValidationResult(String skuCode, boolean valid, ProductValidationFailureReason reason, BigDecimal currentPrice) {
        this.skuCode = skuCode;
        this.valid = valid;
        this.reason = reason;
        this.currentPrice = currentPrice;
    }
    public ItemValidationResult(){}

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public ProductValidationFailureReason getReason() {
        return reason;
    }

    public void setReason(ProductValidationFailureReason reason) {
        this.reason = reason;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}
