package chatclient.scenes;

import chatclient.App;
import chatclient.listviewcells.ServerListCell;
import chatclient.responses.ListRooms;
import chatclient.responses.Room;
import com.google.gson.Gson; // Make sure to have Gson dependency
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;


/**
 * Class used to represent a Server Listing Screen.
 * We should be able to create a new room and join a current room from this screen.
 */
public class Servers implements Screen {
    private final Stage mainStage;
    private ListView<ServerListCell> servers;

    public Servers(Stage s) {
        this.mainStage = s;
        this.servers = new ListView<>();
        this.servers.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> connectToRoom(null));
    }

    /**
     * Builds the Server listing Screen
     * @return root to the Server Listing Screen
     */
    public Pane build() {

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(30, 30, 30, 30));
        grid.setHgap(8);
        grid.setVgap(8);
        refreshServerList(null);
        Button refreshButton = new Button("Refresh");
        refreshButton.setMinWidth(120);
        refreshButton.setOnAction(this::refreshServerList);
        Button createRoomButton = new Button("Create Room");
        createRoomButton.setMinWidth(120);
        createRoomButton.setOnAction(this::showCreateRoomForm);

        Label usernameLabel = new Label("Living Rooms:");
        grid.add(refreshButton, 0, 0);
        grid.add(createRoomButton, 2, 0);
        grid.add(usernameLabel, 0, 1);
        grid.add(servers, 0, 2, 4, 1); // Span across two columns

        return grid;
    }

    /**
     * Fetch active servers from the Chat Server
     * @param ev
     */
    private void refreshServerList(ActionEvent ev) {
        try {
            // Create the HTTP client
            HttpClient client = HttpClient.newHttpClient();

            // Build the request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://csc413.ajsouza.com/api/rooms"))
                    .header("Authorization", "Bearer " + Login.userSession.getToken()) // Include the authorization header
                    .GET() // Set to GET method
                    .build();

            // Execute the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response status code
            if (response.statusCode() == 200) {
                ListRooms listRooms = new Gson().fromJson(response.body(), ListRooms.class);
                List<Room> rooms = listRooms.getRooms();

                // Clear existing items and add new ones
                servers.getItems().clear();
                for (Room room : rooms) {
                    servers.getItems().add(new ServerListCell(room.getRoom_name(), room.getOwner_id(), room.getId(), servers));
                }
            } else {
                System.out.println("Failed to fetch rooms: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connect to the current room selected in the servers listing.
     * Should open a new websocket, and also fetch messages and users for
     * the connecting room.
     * @param ev
     */
    private void connectToRoom(ActionEvent ev) {
        ServerListCell selectedRoom = servers.getSelectionModel().getSelectedItem();
        if (selectedRoom != null) {
            showMessageBox("Connecting to room: " + selectedRoom.getRoomName());
            Platform.runLater(() -> {
                ChatRoom chatRoom = (ChatRoom) App.getScreen("chatroom");
//                chatRoom.setSession(Login.userSession);
                chatRoom.initWebsocket(selectedRoom.getRoomId(), selectedRoom.getOwnerId(), selectedRoom.getRoomName());
                mainStage.setScene(new Scene(chatRoom.build())); // Set the new scene
            });
        }
    }

    /**
     * Show form to create a new room.
     * @param ev
     */
    private void showCreateRoomForm(ActionEvent ev) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Room");
        dialog.setHeaderText("Enter Room Name:");
        dialog.setContentText("Room Name:");

        dialog.showAndWait().ifPresent(this::handleCreateRoom);
    }
    public void showMessageBox(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleCreateRoom(String roomName) {
        String apiUrl = "https://csc413.ajsouza.com/api/rooms";

        try {
            // Create the HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // Create the JSON payload
            String jsonInputString = "{\"room_name\": \"" + roomName + "\"}";

            // Create the HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("Authorization", "Bearer " + Login.userSession.getToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response code
            if (response.statusCode() == 200) {
                showMessageBox("Room created successfully:" +  roomName);
                refreshServerList(null);
            } else {
                showMessageBox("Failed to create room");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessageBox("An error occurred while creating the room");
        }
    }

}
