package com.ecommerce.product;

import java.util.List;

public class ProductValidationResponse {

    private boolean valid;
    private List<ItemValidationResult> results;

    public ProductValidationResponse(boolean valid, List<ItemValidationResult> results) {
        this.valid = valid;
        this.results = results;
    }

    public ProductValidationResponse() {
    }
    public boolean isValid() {
        return valid;
    }
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    public List<ItemValidationResult> getResults() {
        return results;
    }
    public void setResults(List<ItemValidationResult> results) {
        this.results = results;
    }
}
