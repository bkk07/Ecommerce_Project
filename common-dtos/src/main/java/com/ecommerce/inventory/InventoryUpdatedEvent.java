package com.ecommerce.inventory;
public class InventoryUpdatedEvent{
    private String skuCode;
    private Integer newQuantity;
    private boolean isAvailable;
    public InventoryUpdatedEvent (){
    }
    public InventoryUpdatedEvent(String skuCode, Integer newQuantity, boolean isAvailable) {
        this.skuCode = skuCode;
        this.newQuantity = newQuantity;
        this.isAvailable = isAvailable;
    }
    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Integer getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
