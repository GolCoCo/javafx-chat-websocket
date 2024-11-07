package chatclient.scenes;

import javax.websocket.*;
import chatclient.App;
import chatclient.UserSession;
import chatclient.listviewcells.MessageListCell;
import chatclient.listviewcells.ServerListCell;
import chatclient.responses.ListMessages;
import chatclient.responses.ListUsers;
import chatclient.responses.Message;
import chatclient.responses.User;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatRoom implements Screen {

    private final Stage stage;
    private UserSession userSession;
    private ListView<String> users;
    private  List<User> usersList;
    private ListView<MessageListCell> messages;
    private int roomId = 552;
    private TextField messageField;
    private Button sendMsgButton;
    private Session websocketSession;
    private String websocketUrl;

    public ChatRoom(Stage primaryStage) {
        this.stage = primaryStage;
        usersList = new ArrayList<>(){};
        messages = new ListView<>();
        users = new ListView<>();
    }

    /**
     * Builds the Scene for viewing a chat room.
     * @return
     */
    public Pane build() {

        loadMessages();
        loadUsersPane();
        // Create the main layout (GridPane)
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(30, 30, 30, 30));
        grid.setHgap(12);
        grid.setVgap(12);

        Button logoutButton = new Button("Logout");
        logoutButton.setMinWidth(120);
        logoutButton.setOnAction(this::handleLogout);
        logoutButton.setAlignment(Pos.CENTER);
        // Create a VBox for users
        VBox usersPane = new VBox();
        usersPane.setPadding(new Insets(10));
        Button refreshUserButton = new Button("Refresh User");
        refreshUserButton.setMinWidth(120);
        refreshUserButton.setOnAction((e)->{loadUsersPane();});
        usersPane.getChildren().add(refreshUserButton);
        usersPane.getChildren().add(new Label("Users:"));
        usersPane.getChildren().add(users);

        // Create a VBox for messages
        VBox messagesPane = new VBox();
        messagesPane.setPadding(new Insets(10));
        Button refreshMessageButton = new Button("Refresh Message");
        refreshMessageButton.setMinWidth(120);
        refreshMessageButton.setOnAction((e)->{loadMessages();});
        HBox hBox2 = new HBox();
        hBox2.setSpacing(20);
        hBox2.getChildren().add(refreshMessageButton);
        hBox2.getChildren().add(logoutButton);

        messagesPane.getChildren().add(hBox2);
        messagesPane.getChildren().add(new Label("Messages:"));
        messagesPane.getChildren().add(messages);
        HBox sendMsgBox = new HBox();
        sendMsgBox.setPadding(new Insets(4,0,4,0));
        Label sendMsgLabel = new Label("Send Message:");
        messageField = new TextField();
        messageField.setMinWidth(400);
        sendMsgButton = new Button("Send");
        sendMsgButton.setMinWidth(80);
        sendMsgButton.setOnAction(this::handleSendMessage);
        sendMsgBox.getChildren().add(messageField);
        sendMsgBox.getChildren().add(sendMsgButton);

        usersPane.getChildren().add(sendMsgBox);

        // Add the user list and message list to the grid
        grid.add(usersPane, 0, 0); // put users on the left
        grid.add(messagesPane, 1, 0); // put messages on the right

        return grid;
    }

    private void handleSendMessage(ActionEvent ev) {
        sendMessage();
    }

    private void handleLogout(ActionEvent ev){
        Platform.runLater(() -> {
            stage.setScene(new Scene(App.getScreen("servers").build())); // Set the new scene
        });
    }

    /**
     * Set a reference to the WebSocket Session
     * @param s reference to WebSocket Session.
     */
    public void setSession(UserSession s){
        this.userSession = s;
    }

    /**
     * Initializes a connection of a WebSocket to the chat server.
     * @param roomId which room connecting to
     * @param userId who's connecting to the room
     * @param roomName the name of the room.
     * @return whether or not the connection was made.
     */

    public boolean initWebsocket(final int roomId, final int userId, final String roomName) {
        this.roomId = roomId;
        this.websocketUrl = "wss://csc413.ajsouza.com/chat?room_id=" + roomId + "&user_id=" + Login.userSession.getUserId() + "&token=" + Login.userSession.getToken();
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    websocketSession = session;
                    System.out.println("WebSocket connection established!");
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("WebSocket connection closed: " + closeReason);
                    websocketSession = null;
                }
                @OnMessage
                public void onMessage(String msg) {
                    handleIncomingMessage(msg);
                }

                @Override
                public void onError(Session session, Throwable throwable) {
                    System.err.println("WebSocket error: " + throwable.getMessage());
                }
            }, ClientEndpointConfig.Builder.create().build(), new URI(websocketUrl));
            return true; // Connection was successful
        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false; // Connection failed
        }
    }

    /**
     * handles an incoming message from the Chat Server.
     * Gson is needed here to parse the Incoming Message
     * to a WebSocketResponse Type.
     * Depending on the value of event, this function should
     * handling a regular chat message, a join event,
     * and a leave event.
     * @param msg
     */
    public void handleIncomingMessage(String msg) {
        // Assuming msg contains the necessary information to decipher the type of event
        // Parse the message using Gson
        Message incomingMessage = new Gson().fromJson(msg, Message.class);

        // Handle different types of incoming messages
        Platform.runLater(() -> {
            if (incomingMessage.getEventType().equals("message")) {
                messages.getItems().add(new MessageListCell(
                        incomingMessage.getUsername(),
                        incomingMessage.getText(),
                        incomingMessage.getCreated_at(),
                        messages));
                loadMessages();
            } else if (incomingMessage.getEventType().equals("join")) {
                users.getItems().add(incomingMessage.getUsername());
                loadUsersPane();
            } else if (incomingMessage.getEventType().equals("leave")) {
                users.getItems().remove(incomingMessage.getUsername());
                loadUsersPane();
            }
        });
    }

    /**
     * send message to server using the current WebSocket
     * session.
     * @param message to be sent.
     */

    public void sendMessage() {

        if(this.messageField.getText().equals(""))return;
        String userName = users.getSelectionModel().getSelectedItem();
        User user = GetUserByUsername(userName);
        if(user ==null )return;
        this.websocketUrl = "wss://csc413.ajsouza.com/chat?room_id=" + roomId + "&user_id=" + user.getUser_id() + "&token=" + Login.userSession.getToken();
        if (websocketSession != null && websocketSession.isOpen()) {
            try {
                websocketSession.getBasicRemote().sendText(this.messageField.getText());
                messageField.clear();
            } catch (IOException ex) {
                showErrorDialog("Error sending message: " + ex.getMessage());
            }
        } else {
            showErrorDialog("WebSocket session is not open.");
        }
    }

    /**
     * fetch current users in a active room.
     * Users should be added to the  users ListView
     */

    public void loadUsersPane() {
        executeApiCall("https://csc413.ajsouza.com/api/rooms/" + roomId + "/users", response -> {
            ListUsers listUsers = new Gson().fromJson(response, ListUsers.class);
            usersList = listUsers.getUsers();

            Platform.runLater(() -> {
                users.getItems().clear();
                for (User user : usersList) {
                    users.getItems().add(user.getUsername());
                }
            });
        });
    }

    private User GetUserByUsername(String username) {
        for (User user : usersList) {
            if (user.getUsername().equals(username)) {
                return user; // Return the user if the username matches
            }
        }
        return null; // Return null if no user with the given username is found
    }


    /**
     * fetch current messages in a active room.
     * Messages should be added to the messages ListView
     */
    public void loadMessages() {
        executeApiCall("https://csc413.ajsouza.com/api/rooms/" + roomId + "/messages", response -> {
            ListMessages listMessages = new Gson().fromJson(response, ListMessages.class);
            List<Message> messagesList = listMessages.getMessages();

            Platform.runLater(() -> {
                messages.getItems().clear();
                for (Message message : messagesList) {
                    messages.getItems().add(new MessageListCell(
                            message.getUsername(),
                            message.getText(),
                            message.getCreated_at(),
                            messages));
                }
            });
        });
    }

    private void executeApiCall(String apiUrl, ApiResponseHandler handler) {
        HttpClient client = HttpClient.newHttpClient();

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + Login.userSession.getToken())
                .GET() // Use GET request
                .build();

        // Asynchronously send the request
        CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        // Handle the response
        responseFuture.thenAccept(response -> {
            if (response.statusCode() == 200) {
                // Call the handler with the response body
                handler.handle(response.body());
            } else {
                // Handle non-200 response
                showErrorDialog("Failed : HTTP error code : " + response.statusCode());
            }
        }).exceptionally(e -> {
            // Handle exceptions
            Platform.runLater(() -> showErrorDialog("Error fetching data: " + e.getMessage()));
            e.printStackTrace();
            return null;
        });
    }

    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Create an interface for handling API responses
    @FunctionalInterface
    private interface ApiResponseHandler {
        void handle(String response);
    }

}
