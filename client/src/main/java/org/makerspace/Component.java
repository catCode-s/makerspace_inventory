package org.makerspace;

import java.util.List;
import java.util.ArrayList;

// a class that models a single component
public class Component {
    private long id;
    private String name;
    private String description;
    private String category;
    private String brand;
    private String partNumber;
    private String powerRequirements;
    private String documentationUrl;
    private List<StockEntry> stockEntries;
    private boolean requiresCheckout;

    // minimum field component constructor
    public Component(long id, String name, String category, String brand) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.partNumber = null;
        this.powerRequirements = null;
        this.documentationUrl = null;
        this.stockEntries = null;
        this.requiresCheckout = true;
    }

    // full field component constructor
    public Component(long id, String name, String description, String category, String brand,
                     String partNumber, String powerRequirements, String documentationUrl, List<StockEntry> stockEntries, boolean requiresCheckout) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.partNumber = partNumber;
        this.powerRequirements = powerRequirements;
        this.documentationUrl = documentationUrl;
        this.stockEntries = stockEntries;
        this.requiresCheckout = requiresCheckout;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public String getPowerRequirements() {
        return powerRequirements;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public List<StockEntry> getStock() {
        return stockEntries;
    }

    public Integer getQuantity() {
        if (stockEntries == null) {
            return null;
        }

        int total = 0;
        for (StockEntry stock : stockEntries) {
            if (stock.getQuantity() == null) {
                return null;
            }
            total += stock.getQuantity();
        }
        return total;
    }

    public Integer getQuantityAtLocation(String location) {
        if (stockEntries == null) {
            return null;
        }
        int totalQuantity = 0;
        for (StockEntry stock : stockEntries) {
            if (stock.getLocation().equals(location)) {
                if (stock.getQuantity() == null) {
                    return null;
                }
                totalQuantity += stock.getQuantity();
            }
        }
        return totalQuantity;
    }

    public Integer getQuantityAtLocationId(long locationId) {
        if (stockEntries == null) {
            return null;
        }
        int totalQuantity = 0;
        for (StockEntry stock : stockEntries) {
            if (stock.getLocationId() == locationId) {
                if (stock.getQuantity() == null) {
                    return null;
                }
                totalQuantity += stock.getQuantity();
            }
        }
        return totalQuantity;
    }

    public Integer getQuantityAcrossLocations(List<String> locations) {
        if (stockEntries == null) {
            return null;
        }
        int totalQuantity = 0;
        for (StockEntry stock : stockEntries) {
            if (locations.contains(stock.getLocation())) {
                if (stock.getQuantity() == null) {
                    return null;
                }
                totalQuantity += stock.getQuantity();
            }
        }
        return totalQuantity;
    }

    public List<String> getLocations() {
        if (stockEntries == null) {
            return null;
        }
        List<String> locations = new ArrayList<>();
        for (StockEntry stock : stockEntries) {
            locations.add(stock.getLocation());
        }
        return locations;
    }

    public boolean requiresCheckout() {
        return requiresCheckout;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public void setStock(List<StockEntry> stockEntries) {
        this.stockEntries = stockEntries;
    }

    public void setPowerRequirements(String powerRequirements) {
        this.powerRequirements = powerRequirements;
    }

    public void setRequiresCheckout(boolean requiresCheckout) {
        this.requiresCheckout = requiresCheckout;
    }

    public void setDescription(String description) {
        this.description = description;
    } 
}