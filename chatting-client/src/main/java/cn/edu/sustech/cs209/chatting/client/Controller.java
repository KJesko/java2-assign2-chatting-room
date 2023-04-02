package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import java.net.Socket;
import java.util.stream.Collectors;


public class Controller implements Initializable {

    @FXML
    public TextArea inputArea;
    @FXML
    public Label currentUsername;
    @FXML
    public ListView<String> chatList;

    @FXML
    ListView<Message> chatContentList;

    String currentChat;
    Socket client;

    ObjectInputStream in;
    ObjectOutputStream out;
    @FXML
    String username;



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            client = new Socket("localhost", 9090);
            System.out.println(client.getLocalSocketAddress());
            out = new ObjectOutputStream(client.getOutputStream());
            in = new ObjectInputStream(client.getInputStream()); //没被注释时无法打开
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            Dialog<String> dialog = new TextInputDialog();
            dialog.setTitle("Login");
            dialog.setHeaderText(null);
            dialog.setContentText("Username:");
            Optional<String> input = dialog.showAndWait();

            if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
                username = input.get();
                currentUsername.textProperty().bind(Bindings.concat("Current User: ").concat(username));

                ObservableList<String> items = FXCollections.observableArrayList();
                List<String> list = new ArrayList<>();
                items.setAll(list);
                chatList.setItems(items);
                chatList.setCellFactory((ListView<String> l) -> new chatCellFactory());
                chatList.getSelectionModel().selectedItemProperty().addListener(
                        (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                            currentChat = newValue;
                            System.out.println(currentChat);
                        });
                try {
                    String sentByUser = username;
                    String sentToUser = "server";
                    String sentByUrl = client.getLocalSocketAddress().toString();
                    String sentToUrl = client.getRemoteSocketAddress().toString();
                    String data = username + " want to get userSet";
                    Long timestamp = System.currentTimeMillis();
                    Message message = new Message(0, timestamp, sentByUser, sentToUser, sentByUrl, sentToUrl, data);
                    out.writeObject(message);
                    Message userOnlineMessage = (Message) in.readObject();
                    System.out.println(userOnlineMessage.getData());
                    Set<String> userSet = new HashSet<>(Arrays.asList(userOnlineMessage.getData().split(",")));

                    if (username.contains(",")) {
                        System.out.println(userSet);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText(null);
                        alert.setContentText("Commas are not allowed in user names");
                        alert.showAndWait();
                        System.out.println();
                        continue;
                    }

                    if (!userSet.contains(username)) {
                        timestamp = System.currentTimeMillis();
                        data = "login success";
                        Message loginMessage = new Message(1, timestamp, sentByUser, sentToUser, sentByUrl, sentToUrl, data);
                        out.writeObject(loginMessage);

                        break;
                    } else {
                        System.out.println(userSet);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText(null);
                        alert.setContentText("This username has already been taken, please change the username.");
                        alert.showAndWait();
                        System.out.println();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Invalid username " + input + ", exiting");
                Platform.exit();
                break;
            }
        }
        chatContentList.setCellFactory(new MessageCellFactory());
        ObservableList<Message> items = FXCollections.observableArrayList(new ArrayList<>());
        chatContentList.setItems(items);

        Thread t = null;
        try {
            t = new Thread(new SocketListener(client,in ,chatContentList));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        t.start();
    }

    @FXML
    public void createPrivateChat() throws IOException, ClassNotFoundException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();


        // FIXME: get the user list from server, the current user's name should be filtered out
        String sentByUser = username;
        String sentToUser = "server";
        String sentByUrl = client.getLocalSocketAddress().toString();
        String sentToUrl = client.getRemoteSocketAddress().toString();
        String data = username + " want to get userSet";
        Long timestamp = System.currentTimeMillis();
        ObjectInputStream tempIn = new ObjectInputStream(client.getInputStream());

        synchronized (tempIn){
            Message message = new Message(0, timestamp, sentByUser, sentToUser, sentByUrl, sentToUrl, data);
            out.writeObject(message);
            Message userOnlineMessage = (Message) tempIn.readObject();
            System.out.println(userOnlineMessage.getData());
            Set<String> userSet = new HashSet<>(Arrays.asList(userOnlineMessage.getData().split(",")));
            userSet.remove(username);
            userSel.getItems().addAll(userSet);
        }


        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(50);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        ObservableList<String> items = chatList.getItems();
        if (!items.contains(user.get())) {
            items.add(user.get());
        }
        currentChat = user.get();
        System.out.println(items);
        chatList.getSelectionModel().select(items.indexOf(user.get()));
        chatList.setItems(items);


        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        if (inputArea.getText().isEmpty() || Objects.equals(inputArea.getText(), "")) {
            return;
        }

        try {
            System.out.println(inputArea.getText());
            String sentByUser = username;
            String sentToUser = currentChat;
            String sentByUrl = client.getLocalSocketAddress().toString();
            System.out.println(sentByUrl);
            String sentToUrl = client.getRemoteSocketAddress().toString();
            String data = inputArea.getText();
            inputArea.setText(null);
            Long timestamp = System.currentTimeMillis();
            Message message = new Message(2, timestamp, sentByUser, sentToUser, sentByUrl, sentToUrl, data);
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    }


    private static class chatCellFactory extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setText(null);
            } else {
                String[] chatUserArr = item.split(",");
                List<String> chatUserList = Arrays.stream(chatUserArr).sorted().collect(Collectors.toList());
                StringBuilder stringBuilder = new StringBuilder();

                for (String name : chatUserList) {
                    stringBuilder.append(name);
                    stringBuilder.append(",");
                }
                String text = stringBuilder.substring(0, stringBuilder.length() - 1);

                if (chatUserList.size() >= 3) {
                    text += "...";
                }
                text += " (" + chatUserList.size() + ")";

                setText(text);
            }
        }
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentByUser());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentByUser())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    private class SocketListener implements Runnable {

        private final Socket socket;
        private final ListView<Message> chatContentList;

        private ObjectInputStream in;
//        private final ObjectOutputStream out;

        public SocketListener(Socket socket, ObjectInputStream in, ListView<Message> chatContentList) throws IOException {
            this.socket = socket;
            this.chatContentList = chatContentList;
//            this.out = new ObjectOutputStream(socket.getOutputStream());
//            this.in = in;
        }

        public void run() {
            try {
                this.in = new ObjectInputStream(this.socket.getInputStream());
                synchronized (in) {
                    Message message = (Message) in.readObject();
                    ObservableList<Message> items = chatContentList.getItems();
                    items.add(message);
                    if (message.getType() == 2) {

                        Platform.runLater(() -> Controller.this.chatContentList.setItems(items));
                    }
                }


//                String line;
//                while ((line = in.readLine()) != null) {
//                    String finalLine = line;
//
//                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}






