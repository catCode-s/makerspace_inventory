package org.makerspace;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
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

        Label accountStatus = new Label("Browsing as guest");
        Button loginButton = new Button("Log in");
        Button logoutButton = new Button("Log out");

        logoutButton.disableProperty().bind(
            loginButton.disableProperty().not()
        );

        HBox accountBar = new HBox(
            10, accountStatus, loginButton, logoutButton
        );

        loginButton.setOnAction(event ->
            showLogin(stage, api, accountStatus, loginButton)
        );

        logoutButton.setOnAction(event -> {
            logoutButton.setText("Logging out…");
            accountBar.setDisable(true);

            api.logout().whenComplete((ignored, error) -> {
                Platform.runLater(() -> {
                    accountBar.setDisable(false);
                    logoutButton.setText("Log out");

                    if (error != null) {
                        accountStatus.setText("Logout failed. Try again.");
                    } else {
                        loginButton.setDisable(false);
                        accountStatus.setText("Browsing as guest");
                    }
                });
            });
        });

        Runnable refreshAccount = () -> {
            api.currentUser().whenComplete((user, error) -> {
                Platform.runLater(() -> {
                    if (error != null) {
                        accountStatus.setText("Account status unavailable");
                        return;
                    }

                    loginButton.setDisable(user != null);

                    accountStatus.setText(
                        user == null
                            ? "Browsing as guest"
                            : "Signed in as " + user.displayName()
                                + " (" + user.role() + ")"
                    );
                });
            });
        };

        stage.focusedProperty().addListener((observable, previous, focused) -> {
            if (focused && !accountBar.isDisabled()) {
                refreshAccount.run();
            }
        });

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

        VBox root = new VBox(12, accountBar, searchBar, status, results, details, documentationLink);
        root.setPadding(new Insets(20));
        VBox.setVgrow(results, Priority.ALWAYS);

        stage.setTitle("Makerspace Inventory");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
        search.run();
    }

    private void showLogin(
            Stage owner,
            InventoryApi api,
            Label accountStatus,
            Button loginButton) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Log in");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Label message = new Label();
        message.setWrapText(true);

        Button submit = new Button("Log in");

        Runnable login = () -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                message.setText("Enter your email and password.");
                return;
            }

            submit.setDisable(true);
            emailField.setDisable(true);
            passwordField.setDisable(true);
            message.setText("Logging in…");

            // Keep the dialog open until the request completes.
            dialog.setOnCloseRequest(event -> event.consume());

            api.login(email, password).whenComplete((user, error) -> {
                Platform.runLater(() -> {
                    passwordField.clear();
                    dialog.setOnCloseRequest(null);

                    if (error != null) {
                        Throwable cause = error;
                        while (cause.getCause() != null) {
                            cause = cause.getCause();
                        }

                        message.setText(
                            cause instanceof IllegalStateException
                                ? cause.getMessage()
                                : "Could not log in. Check the backend connection."
                        );

                        submit.setDisable(false);
                        emailField.setDisable(false);
                        passwordField.setDisable(false);
                    } else {
                        accountStatus.setText("Signed in as " + user.displayName() + " (" + user.role() + ")");
                        loginButton.setDisable(true);
                        dialog.close();
                    }
                });
            });
        };

        submit.setOnAction(event -> login.run());
        passwordField.setOnAction(event -> {
            if (!submit.isDisabled()) {
                login.run();
            }
        });

        VBox content = new VBox(
            12, new Label("Email"), emailField,
            new Label("Password"), passwordField, message, submit
        );
        content.setPadding(new Insets(20));

        dialog.setScene(new Scene(content, 380, 300));
        dialog.show();
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