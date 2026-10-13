package org.makerspace;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// JavaFX interface for searching components and their details
public class InventoryApp extends Application {
    @Override
    public void start(Stage stage) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search components…");

        Button searchButton = new Button("Search");

        HBox searchBar = new HBox(10, searchField, searchButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label status = new Label("Enter a name, part number, or tag.");
        ListView<Component> results = new ListView<>();

        // display each components name and stock summary
        results.setCellFactory(list -> new ListCell<Component>() {
            @Override
            protected void updateItem(Component component, boolean empty) {
                super.updateItem(component, empty);
                setGraphic(null);

                if (empty || component == null) {
                    setText(null);
                    return;
                }

                Integer quantity = component.getQuantity();
                String summary;

                if (component.getStock() == null || component.getStock().isEmpty())
                    summary = "No stock records";
                else if (quantity == null) 
                    summary = "Approximate stock — select for details";
                else
                    summary = quantity + " available";

                setText(component.getName() + " — " + summary);
            }
        });

        TextArea details = new TextArea();
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(8);
        details.setPromptText("Select a component to view its details.");

        Hyperlink documentationLink = new Hyperlink("Open documentation");
        documentationLink.setDisable(true);

        documentationLink.setOnAction(event -> {
            Component selected = results.getSelectionModel().getSelectedItem();

            if (selected != null) {
                String url = selected.getDocumentationUrl();

                if (url != null && !url.isBlank())
                    getHostServices().showDocument(url);
            }
        });

        // update the details and documentation link when a component is selected
        results.getSelectionModel().selectedItemProperty().addListener(
            (observable, previous, component) -> {
                documentationLink.setDisable(
                    component == null
                        || component.getDocumentationUrl() == null
                        || component.getDocumentationUrl().isBlank()
                );
                documentationLink.setVisited(false);

                if (component == null) {
                    details.clear();
                    return;
                }

                StringBuilder text = new StringBuilder();
                text.append(component.getName()).append("\n");
                text.append("Brand: ")
                    .append(display(component.getBrand())).append("\n");
                text.append("Category: ")
                    .append(display(component.getCategory())).append("\n");
                text.append("Part number: ")
                    .append(display(component.getPartNumber())).append("\n");
                text.append("Description: ")
                    .append(display(component.getDescription())).append("\n");
                text.append("Power requirements: ")
                    .append(display(component.getPowerRequirements())).append("\n");
                text.append("Checkout required: ")
                    .append(component.requiresCheckout() ? "Yes" : "No")
                    .append("\n\nStock:\n");

                if (component.getStock() == null || component.getStock().isEmpty()) {
                    text.append("No stock records\n");
                } else {
                    for (StockEntry stock : component.getStock()) {
                        text.append(display(stock.getLocation())).append(": ");

                        if (stock.getQuantity() != null)
                            text.append(stock.getQuantity());
                        else
                            text.append(display(stock.getStockLevel()));

                        text.append("\n");
                    }
                }

                details.setText(text.toString());
                details.positionCaret(0);
            }
        );

        InventoryApi api = new InventoryApi();

        Runnable search = () -> {
            searchButton.setDisable(true);
            searchField.setDisable(true);
            results.getItems().clear();
            details.clear();
            documentationLink.setDisable(true);
            status.setText("Searching…");

            api.search(searchField.getText().trim())
                .whenComplete((matches, error) -> {
                    // apply HTTP response to the UI
                    Platform.runLater(() -> {
                        searchButton.setDisable(false);
                        searchField.setDisable(false);

                        if (error != null) {
                            status.setText("Search failed. Check that the backend is running.");
                            error.printStackTrace();
                        } else {
                            results.getItems().setAll(matches);
                            status.setText(
                                matches.isEmpty()
                                    ? "No components found."
                                    : matches.size() + " component(s) found."
                            );
                        }
                    });
                });
        };

        searchButton.setOnAction(event -> search.run());
        searchField.setOnAction(event -> search.run());

        VBox root = new VBox(12, searchBar, status, results, details, documentationLink);
        root.setPadding(new Insets(20));
        VBox.setVgrow(results, Priority.ALWAYS);

        stage.setTitle("Makerspace Inventory");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
        search.run();
    }

    private static String display(String value) {
        return value == null || value.isBlank()
            ? "Not specified"
            : value;
    }

    public static void main(String[] args) {
        launch(args);
    }
}