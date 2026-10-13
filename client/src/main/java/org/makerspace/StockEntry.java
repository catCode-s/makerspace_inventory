package org.makerspace;

import java.util.List;

// a class that models a quantity of a component at one location
public class StockEntry {
    private long inventoryId;
    private long locationId;
    private String location;
    private String stockLevel;
    private Integer quantity;

    public StockEntry(long inventoryId, long locationId, String location,
                       String stockLevel, Integer quantity) {
        this.inventoryId = inventoryId;
        this.locationId = locationId;
        this.location = location;
        this.stockLevel = stockLevel;
        this.quantity = quantity;
    }

    public long getInventoryId() {
        return inventoryId;
    }

    public long getLocationId() {
        return locationId;
    }

    public String getLocation() {
        return location;
    }

    public String getStockLevel() {
        return stockLevel;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setInventoryId(long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }   

    public void setLocation(String location) {
        this.location = location;
    }

    public void setStockLevel(String stockLevel) {
        this.stockLevel = stockLevel;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}