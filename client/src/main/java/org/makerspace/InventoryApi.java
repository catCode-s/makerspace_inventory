package org.makerspace;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// sends asynchronous search requests to backend, converting JSON responses to Component objects
public class InventoryApi {
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public CompletableFuture<List<Component>> search(String search) {
        String encoded = URLEncoder.encode(search, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://127.0.0.1:8080/api/components?search=" + encoded
            ))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        return client.sendAsync(
            request, HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                    "Server returned HTTP " + response.statusCode()
                );
            }

            List<Component> results = new ArrayList<>();

            for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonArray()) {
                JsonObject component = element.getAsJsonObject();
                List<StockEntry> stockEntries = new ArrayList<>();

                for (JsonElement stockElement : component.getAsJsonArray("stock")) {
                    JsonObject stock = stockElement.getAsJsonObject();

                    stockEntries.add(new StockEntry(
                        stock.get("inventory_id").getAsLong(),
                        stock.get("location_id").getAsLong(),
                        stock.get("location").getAsString(),
                        nullableString(stock, "stock_level"),
                        stock.get("quantity").isJsonNull()
                            ? null
                            : stock.get("quantity").getAsInt()
                    ));
                }

                results.add(new Component(
                    component.get("id").getAsLong(),
                    component.get("name").getAsString(),
                    nullableString(component, "description"),
                    nullableString(component, "category"),
                    nullableString(component, "brand"),
                    nullableString(component, "part_number"),
                    nullableString(component, "power_requirements"),
		            nullableString(component, "documentation_url"),
                    stockEntries,
                    component.get("requires_checkout").getAsBoolean()
                ));
            }
            return results;
        });
    }

    // preserve null JSON fields as Java null
    private static String nullableString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull()
            ? null
            : value.getAsString();
    }
}
