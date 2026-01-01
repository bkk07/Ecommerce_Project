package com.ecommerce.inventory;
import java.util.ArrayList;
import java.util.List;

public class StockUpdate {
    List<StockItem> stockItems= new ArrayList<>();
    public StockUpdate(){}
    public StockUpdate(List<StockItem> stockItems) {
        this.stockItems = stockItems;
    }

    public List<StockItem> getStockItems() {
        return stockItems;
    }

    public void setStockItems(List<StockItem> stockItems) {
        this.stockItems = stockItems;
    }
}
