package chatclient.scenes;

import chatclient.App;
import chatclient.UserSession;
import chatclient.responses.LoginResponse;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Login implements Screen {
    public static UserSession userSession;
    private Stage mainStage;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label messageLabel;

    public Login(Stage mainStage) {
        this.mainStage = mainStage;
    }

    /**
     * Builds the Login Scene and returns the root of that scene.
     *
     * @return A built Scene
     */
    public Pane build() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(40, 50, 20, 50));
        grid.setHgap(8);
        grid.setVgap(8);

        // Username Label and Field
        Label usernameLabel = new Label("Username:");
        usernameField = new TextField();
        usernameField.setMinWidth(140);
        usernameField.setText("eogunrinu");
        grid.add(usernameLabel, 0, 0);
        grid.add(usernameField, 1, 0);

        // Password Label and Field
        Label passwordLabel = new Label("Password:");
        passwordField = new PasswordField();
        passwordField.setText("2897@sfsu");
        passwordField.setMinWidth(140);
        grid.add(passwordLabel, 0, 1);
        grid.add(passwordField, 1, 1);

        // Message Label
        messageLabel = new Label();
        grid.add(messageLabel, 0, 3, 2, 1);

        HBox buttonBox = new HBox(10); // Spacing between buttons
        buttonBox.setPadding(new Insets(0, 50, 0, 0)); // Add right padding

        // Login Button
        Button loginButton = new Button("Login");
        loginButton.setMinWidth(80);
        loginButton.setOnAction(this::handleLogin);
        buttonBox.getChildren().add(loginButton); // Add to HBox

        // Reset Button
        Button resetButton = new Button("Reset");
        resetButton.setMinWidth(80);
        resetButton.setOnAction(this::handleReset);
        buttonBox.getChildren().add(resetButton); // Add to HBox

        // Add the HBox to the grid
        grid.add(buttonBox, 1, 2, 2, 1); // span across 2 columns if necessary

        return grid;
    }

    /**
     * Used to handle login button click.
     * This function should send username and password to the server
     * and then depending on the response should either display
     * invalid login credentials or transition to the Servers Screen.
     *
     * @param ev Event from button click
     */
    private void handleLogin(ActionEvent ev) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String url = "https://csc413.ajsouza.com/users/login/app";
        String jsonPayload = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    System.out.println(response);
                    Gson gson = new Gson();
                    LoginResponse loginResponse = gson.fromJson(response, LoginResponse.class);
                    if (loginResponse.getStatus().equals("error")) {
                        Platform.runLater(() -> {
                            messageLabel.setText("Invalid login credentials. Please try again.");
                        });

                    } else {
                        // Create UserSession on successful login
                        userSession = UserSession.getInstance();
                        userSession.initSession(loginResponse.getUsername(), loginResponse.getUser_id(), loginResponse.getToken());
                        // Transition to Servers Screen
                        Platform.runLater(() -> {
                            mainStage.setScene(new Scene(App.getScreen("servers").build())); // Set the new scene
                        });

                    }
                })
                .exceptionally(e -> {
                    messageLabel.setText("Invalid login credentials. Please try again.");
                    System.out.println("Error: " + e.getMessage());
                    return null;
                });
    }

    /**
     * Used to handle reset button click.
     *
     * @param ev Event from button click
     */
    private void handleReset(ActionEvent ev) {
        usernameField.clear();
        passwordField.clear();
        messageLabel.setText("");
    }
}
