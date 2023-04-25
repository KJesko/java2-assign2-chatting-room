package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import java.net.Socket;
import java.util.stream.Collectors;


public class Controller implements Initializable {

  public ImageView userHeadImg;
  public Tooltip createPrivateChatTooltip;
  public Button createPrivateChatButton;
  public Button createGroupChatButton;
  public Tooltip createGroupChatTooltip;
  public Label nameLabel;

  Scene loginScene;

  Scene mainScene;

  Stage stage;

  LoginController loginController;

  Stage loginStage;

  @FXML
  public TextArea inputArea;
  @FXML
  public Label currentUsername;
  @FXML
  public ListView<String> chatList;
  public Label currentOnlineCnt;

  @FXML
  ListView<Message> chatContentList;

  ObservableList<Message> messageItems = FXCollections.observableArrayList();

  String currentChat;
  Socket client;

  Set<String> userSet = new HashSet<>();

  ObjectInputStream in;
  ObjectOutputStream out;
  @FXML
  String username;

  String pwd;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {


    try {
      client = new Socket("localhost", 9090);
      System.out.println(client.getLocalSocketAddress());
      out = new ObjectOutputStream(client.getOutputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

//        Thread shutdownHook = new Thread(() -> {
//            Message message = new Message();
//            message.setType(4);
//            message.setSentByUser(username);
//            message.setData("Abnormal exit");
//            System.out.println("Abnormal exit");
//            try {
//                out.writeObject(message);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
//        Runtime.getRuntime().addShutdownHook(shutdownHook);


    Thread clientListener = null;
    try {
      clientListener = new Thread(new clientSocket(client, userSet));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    clientListener.start();
    loginStage = new Stage();

    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
      Parent root = loader.load();
      loginController = loader.getController();
      loginController.setController(this);

      loginStage.setScene(new Scene(root, 400, 300));
      loginStage.setOnCloseRequest(event -> {
        stage.close();
        Platform.exit();
        System.exit(0);
      });
      loginController.setLoginStage(loginStage);

      // 从loginController中获取数据，执行登录逻辑
      // ...
    } catch (IOException e) {
      e.printStackTrace();
    }
    loginStage.showAndWait();


    currentUsername.textProperty().bind(Bindings.concat("Current User: ").concat(username));

    ObservableList<String> items = FXCollections.observableArrayList();
    List<String> list = new ArrayList<>();
    items.setAll(list);
    chatList.setItems(items);
    chatList.getSelectionModel().selectedItemProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
              currentChat = newValue;
              ObservableList<Message> filterMessageItems = FXCollections.observableArrayList(messageItems.stream().filter(e -> e.getBelongToChat().equals(this.currentChat)).collect(Collectors.toList()));
              if (filterMessageItems.size() != 0) {
                Message latestMessage = filterMessageItems.get(filterMessageItems.size() - 1);
                latestMessage.setHaveRead(true);
                Message confirmMessage = new Message();
                confirmMessage.setType(7);
                confirmMessage.setSentByUser(username);
                confirmMessage.setBelongToUser(username);
                confirmMessage.setBelongToChat(currentChat);
                confirmMessage.setData("confirm read");
                confirmMessage.setTimestamp(System.currentTimeMillis());
                try {
                  out.writeObject(confirmMessage);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
              chatContentList.setItems(filterMessageItems);
              chatContentList.scrollTo(chatContentList.getItems().size() - 1);
              chatList.refresh();
            });
    chatList.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 1) {
          int index = chatList.getSelectionModel().getSelectedIndex();
          chatList.getSelectionModel().select(index);
        }

      }
    });
    chatList.setBackground(Background.EMPTY);
    chatList.setFocusTraversable(false);
    chatList.setMouseTransparent(false);
    chatList.setCellFactory(param -> {
      ListCell<String> cell = new ChatCellFactory();
      cell.setOnMouseClicked(event -> {
        if (event.getClickCount() == 1) {
          if (cell.getItem() != null) {
            if (cell.getIndex() == chatList.getSelectionModel().getSelectedIndex()) {

              ObservableList<Message> filterMessageItems = FXCollections.observableArrayList(messageItems.stream().filter(e -> e.getBelongToChat().equals(this.currentChat)).collect(Collectors.toList()));
              if (filterMessageItems.size() != 0) {
                Message latestMessage = filterMessageItems.get(filterMessageItems.size() - 1);
                latestMessage.setHaveRead(true);
                Message confirmMessage = new Message();
                confirmMessage.setType(7);
                confirmMessage.setSentByUser(username);
                confirmMessage.setBelongToUser(username);
                confirmMessage.setBelongToChat(currentChat);
                confirmMessage.setData("confirm read");
                confirmMessage.setTimestamp(System.currentTimeMillis());
                try {
                  out.writeObject(confirmMessage);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
              chatContentList.setItems(filterMessageItems);
              chatContentList.scrollTo(chatContentList.getItems().size() - 1);

              chatList.refresh();
              System.out.println("Selected item clicked!");
            }
          }
        }
      });
      return cell;
    });
    chatList.setPadding(new Insets(5));
    chatList.setStyle("-fx-border-width: 0px; -fx-background-color: transparent;-fx-control-inner-background: transparent;");
//        chatList.addEventFilter(MouseEvent.MOUSE_PRESSED, Event::consume);


//        chatContentList.setCellFactory(new MessageCellFactory());
    chatContentList.setBackground(Background.EMPTY);
    chatContentList.setFocusTraversable(true);
    chatContentList.setMouseTransparent(false);
    chatContentList.setCellFactory(param -> {
      ListCell<Message> cell = new ChatBubbleCell();
      cell.setStyle("-fx-background-color: transparent; -fx-selection-bar: transparent;");
      return cell;
    });
    chatContentList.setPadding(new Insets(5));
    chatContentList.setStyle("-fx-border-width: 0px; -fx-background-color: transparent;-fx-control-inner-background: transparent;");

    chatContentList.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
          if (chatContentList.getSelectionModel().getSelectedItem().getType() == 6) {
            String messageData = chatContentList.getSelectionModel().getSelectedItem().getData();
            String fileId = messageData.substring(0, messageData.indexOf(":"));
            String oldName = messageData.substring(messageData.indexOf(":") + 1);
            Path selectedFilePath = Paths.get("file/" + fileId);
            Path newPath = Paths.get("C:\\Users\\HUAWEI\\Desktop\\chatting-download\\" + oldName);

            try {
              Files.copy(selectedFilePath, newPath, StandardCopyOption.REPLACE_EXISTING);
              Stage stage = new Stage();
              HBox box = new HBox(50);
              box.setAlignment(Pos.CENTER);
              box.setPadding(new Insets(20, 20, 20, 20));
              Label label = new Label("文件下载完成！请到此路径下查看：\n" + newPath.toString());
              box.getChildren().addAll(label);
              stage.setScene(new Scene(box, 350, 130));
              stage.initModality(Modality.APPLICATION_MODAL);
              stage.setTitle("提示");
              stage.showAndWait();
            } catch (IOException ex) {
              throw new RuntimeException(ex);
            }

          }
        }

      }
    });

//        chatContentList.addEventFilter(MouseEvent.MOUSE_PRESSED, Event::consume);


    //鼠标悬停在按钮上时
    createPrivateChatButton.setOnMouseEntered(e -> {
      createPrivateChatTooltip.show(createPrivateChatButton, e.getScreenX(), e.getScreenY() + 10);
      createPrivateChatButton.setStyle("-fx-background-color: rgb(175,229,255)");
    });
    //鼠标离开按钮时
    createPrivateChatButton.setOnMouseExited(e -> {
      createPrivateChatTooltip.hide();
      createPrivateChatButton.setStyle("-fx-background-color: rgb(0,159,230)");

    });
    createGroupChatButton.setOnMouseEntered(e -> {
      createGroupChatTooltip.show(createPrivateChatButton, e.getScreenX(), e.getScreenY() + 10);
      createGroupChatButton.setStyle("-fx-background-color: rgb(175,229,255)");
    });
    //鼠标离开按钮时
    createGroupChatButton.setOnMouseExited(e -> {
      createGroupChatTooltip.hide();
      createGroupChatButton.setStyle("-fx-background-color: rgb(0,159,230)");


    });


    nameLabel.setText(username);
    InputStream isImg = null;
    try {
      isImg = loadImg(username);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Image headImage = new Image(isImg);


    userHeadImg.setImage(headImage);

    userHeadImg.setClip(new Circle(25, 25, 25));
    userHeadImg.setStyle("-fx-padding: 10px");
//        userHeadImg.setPreserveRatio(true);
    userHeadImg.setFitWidth(50);
    userHeadImg.setFitHeight(50);

    userHeadImg.setOnMouseClicked(e -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("选择图片");
      FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("图片文件 (*.jpg, *.png)", "*.jpg", "*.png");
      fileChooser.getExtensionFilters().add(extFilter);
      File selectedFile = fileChooser.showOpenDialog(stage);
      if (selectedFile != null) {
        Image image = new Image(selectedFile.toURI().toString());
        System.out.println(selectedFile.toURI());
        userHeadImg.setImage(image);


        Path savePath = Paths.get("allUser/" + username);
        // 获取上传文件名


        String oldName = selectedFile.getName();
        String extension = oldName.substring(oldName.lastIndexOf("."));

        // 生成新的文件名
        Path newPath = Paths.get(savePath + "/head" + extension);
        chatContentList.refresh();
        //TODO:发送一个refresh消息，data为chatlist中去重后的所有人，type==3
        Message message = new Message();
        Set<String> chatWithThisUserSet = new HashSet<>();
        for (String s : chatList.getItems()) {
          chatWithThisUserSet.addAll(Arrays.stream(s.split(",")).collect(Collectors.toList()));
        }
        message.setSentByUser(username);
        message.setType(3);
        String data = chatWithThisUserSet.toString();
        message.setData(data.substring(1, data.length() - 1).replace(", ", ","));
        try {
          out.writeObject(message);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }


        System.out.println(newPath.toString());

        try {
          Files.copy(selectedFile.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }

      }

    });


    inputArea.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        if (event.isControlDown() && !event.isShiftDown()) {
          // handle enter+ctrl key press
          inputArea.appendText("\n");
          System.out.println("Enter+Ctrl pressed");
        } else if (event.isShiftDown() && !event.isControlDown()) {
          // handle enter+shift key press
          inputArea.appendText("\n");
          System.out.println("Enter+Shift pressed");
        } else {
          // handle enter key press
          doSendMessage();
          System.out.println("Enter pressed");
          event.consume();
        }
      }
    });


    stage.setOnCloseRequest(event -> {
      Message message = new Message();
      message.setType(4);
      message.setTimestamp(System.currentTimeMillis());
      message.setSentByUser(username);
      message.setSendToUser("server");
      message.setSentByUrl(client.getLocalSocketAddress().toString());
      message.setSendToUrl(client.getRemoteSocketAddress().toString());
      message.setData("disconnect");
      try {
        out.writeObject(message);
        if (client != null) {
          client.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }


      stage.close();
      Platform.exit();
      System.exit(0);
    });

    stage.show();

  }

  public void closeLoginStage() {
    loginStage.close();
  }

  @FXML
  public void createPrivateChat() throws IOException, ClassNotFoundException, InterruptedException {
    AtomicReference<String> user = new AtomicReference<>();

    Stage stage = new Stage();
    ComboBox<String> userSel = new ComboBox<>();


    // FIXME: get the user list from server, the current user's name should be filtered out

    synchronized (this.userSet) {

      userSet.remove(username);
      userSel.getItems().addAll(userSet);
      userSet.add(username);

    }


    Button okBtn = new Button("OK");
    okBtn.setPrefWidth(50);
    okBtn.setOnAction(e -> {
      user.set(userSel.getSelectionModel().getSelectedItem());
      if (user.get() == null) {

      } else {

        stage.close();
        ObservableList<String> items = chatList.getItems();
        if (!items.contains(user.get())) {
          items.add(user.get());
        }
        currentChat = user.get();
        System.out.println(items);
        chatList.getSelectionModel().select(items.indexOf(user.get()));
        chatList.setItems(items);
        System.out.println("chatList contains:" + items);
      }

    });
    userSel.setPrefWidth(100);

    HBox box = new HBox(50);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box, 300, 200));
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setTitle("Create Private Chat");
    stage.showAndWait();


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
  public void createGroupChat() throws IOException, InterruptedException {
    AtomicReference<String> user = new AtomicReference<>();
    Stage stage = new Stage();
    Set<String> selectedUserSet = new HashSet<>();
    ListView<CheckBox> userSel = new ListView<>(FXCollections.observableArrayList(new ArrayList<>()));


    // FIXME: get the user list from server, the current user's name should be filtered out

    synchronized (this.userSet) {
      userSet.remove(username);
      ObservableList<CheckBox> items = userSel.getItems();
      for (String userName : userSet) {
        CheckBox checkBox = new CheckBox(userName);
        items.add(checkBox);
        checkBox.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
          if (checkBox.isSelected()) {
            selectedUserSet.add(checkBox.getText());
          } else {
            selectedUserSet.remove(checkBox.getText());
          }

        });
      }
      userSet.add(username);
      userSel.setItems(items);
    }


    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      String userSelectedString = selectedUserSet.toString();
      System.out.println(user.get());
      user.set(userSelectedString.substring(1, userSelectedString.length() - 1).replace(", ", ","));
      stage.close();
      ObservableList<String> items = chatList.getItems();
      if (!items.contains(user.get())) {
        items.add(user.get());
      }
      currentChat = user.get();
      chatList.getSelectionModel().select(items.indexOf(user.get()));
      chatList.setItems(items);
      System.out.println("chatList contains:" + items);
    });

    HBox box = new HBox(50);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setTitle("Create Group Chat");
    stage.showAndWait();


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
    if (currentChat == null || !chatList.getItems().contains(currentChat)) {
      System.out.println(currentChat + " is not in chatList: " + chatList.getItems());
      return;
    }
    String s = String.valueOf(inputArea.getText());
    System.out.println(inputArea.getText());

    if (s.replace("\n", "").equals("")) {
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
      Message message = new Message(0, timestamp, sentByUser, sentToUser, sentByUrl, sentToUrl, data);
      message.setBelongToUser(sentByUser);
      message.setBelongToChat(sentToUser);
      message.setHaveRead(true);

      synchronized (this.messageItems) {
        messageItems.add(message);
        System.out.println("messageItems.size():" + messageItems.size());
        List<Message> list = messageItems.stream().filter(e -> e.getBelongToChat().equals(currentChat)).collect(Collectors.toList());
        ObservableList<Message> filterMessageItems = FXCollections.observableArrayList(list);
        System.out.println("filterMessageItems.size():" + filterMessageItems.size());
        chatContentList.setItems(filterMessageItems);
        ObservableList<String> chatListItems = chatList.getItems();
        String tempCurrentChat = currentChat;
        chatListItems.remove(currentChat);
        chatListItems.add(0, tempCurrentChat);
        chatList.setItems(chatListItems);
        currentChat = tempCurrentChat;
        chatList.getSelectionModel().selectFirst();
//        chatContentList.getSelectionModel().selectLast();
        chatContentList.scrollTo(chatContentList.getItems().size() - 1);

      }


      out.writeObject(message);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

  }

  public void doSendFile(ActionEvent actionEvent) {

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("选择文件");
    File selectedFile = fileChooser.showOpenDialog(stage);
    if (selectedFile != null) {
      Path savePath = Paths.get("file");
      // 获取上传文件名
      String oldName = selectedFile.getName();
      String fileId = UUID.randomUUID().toString();

      // 生成新的文件名
      Path newPath = Paths.get("file/" + fileId);


      Message message = new Message();
      message.setTimestamp(System.currentTimeMillis());
      message.setBelongToUser(username);
      message.setBelongToChat(currentChat);
      message.setSentByUser(username);
      message.setSendToUser(currentChat);
      message.setType(6);
      message.setData(fileId + ":" + oldName);
      message.setHaveRead(true);

      synchronized (this.messageItems) {
        messageItems.add(message);
        System.out.println("messageItems.size():" + messageItems.size());
        List<Message> list = messageItems.stream().filter(e -> e.getBelongToChat().equals(currentChat)).collect(Collectors.toList());
        ObservableList<Message> filterMessageItems = FXCollections.observableArrayList(list);
        System.out.println("filterMessageItems.size():" + filterMessageItems.size());
        chatContentList.setItems(filterMessageItems);
        ObservableList<String> chatListItems = chatList.getItems();
        String tempCurrentChat = currentChat;
        chatListItems.remove(currentChat);
        chatListItems.add(0, tempCurrentChat);
        chatList.setItems(chatListItems);
        currentChat = tempCurrentChat;
        chatList.getSelectionModel().selectFirst();
        chatContentList.getSelectionModel().selectLast();
        chatContentList.scrollTo(chatContentList.getItems().size() - 1);

      }


      try {
        out.writeObject(message);
        System.out.println(message);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }

      try {
        Files.copy(selectedFile.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }

    }

  }


  private class ChatCellFactory extends ListCell<String> {

    private HBox hbox = new HBox();
    private ImageView headPortrait = new ImageView();
    private Text nameText = new Text();
    private Label messageLabel = new Label();
    private Text timeText = new Text();
    private StackPane countPane = new StackPane();
    private Label countLabel = new Label();

    public ChatCellFactory() {
      super();
      hbox.setSpacing(10);
      hbox.setAlignment(Pos.CENTER_LEFT);
      headPortrait.setFitHeight(40);
      headPortrait.setFitWidth(40);

      nameText.setFont(Font.font("System", 14));
      messageLabel.setFont(Font.font("System", 10));
      messageLabel.setTextFill(Color.GRAY);
      timeText.setFont(Font.font("System", 10));
      timeText.setFill(Color.GRAY);
//            countLabel.setAlignment(Pos.CENTER_RIGHT);
//            countPane.getChildren().add(countLabel);
      countPane.getStyleClass().add("count-pane");
      countPane.setVisible(false);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
      super.updateItem(item, empty);
      if (item == null || empty) {
        setText(null);
        setGraphic(null);
      } else {
        String[] chatUserArr = item.split(",");
        List<String> chatUserList = Arrays.stream(chatUserArr).sorted().collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();

        for (String name : chatUserList) {
          stringBuilder.append(name);
          stringBuilder.append(",");
        }
        String text = stringBuilder.substring(0, stringBuilder.length() - 1);

        if (chatUserList.size() > 3) {
          text += "...";
        }
        if (chatUserList.size() > 1) {
          text += " (" + chatUserList.size() + ")";
        }
        // 设置头像
        double AVATAR_SIZE = 40;

        InputStream isImg = null;
        try {
          isImg = loadImg(item);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        headPortrait = new ImageView(new Image(isImg, AVATAR_SIZE, AVATAR_SIZE, true, true));
        headPortrait.setClip(new Circle(AVATAR_SIZE / 2, AVATAR_SIZE / 2, AVATAR_SIZE / 2));
        headPortrait.setFitWidth(AVATAR_SIZE);
        headPortrait.setFitHeight(AVATAR_SIZE);
        // 设置名字和消息记录
        nameText.setText(text);

        // 设置时间


        // 设置未读消息数量


        VBox vbox1 = new VBox();
        vbox1.getChildren().addAll(nameText, messageLabel);
        vbox1.setAlignment(Pos.CENTER_LEFT);

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(10, 5, 10, 5));
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(10);

        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Message> messages = Controller.this.messageItems.stream().filter((m) -> m.getBelongToChat().equals(item)).collect(Collectors.toList());
        if (messages.size() != 0) {
          Message latestMessage = messages.get(messages.size() - 1);
          Long timestamp = latestMessage.getTimestamp();
          String dateStr = dateformat.format(timestamp);
          String day = dateStr.substring(0, 10);
          String minute = dateStr.substring(11, 16);
          if (System.currentTimeMillis() - timestamp < 1000 * 60 * 60) {
            timeText = new Text(minute);
          } else {
            timeText = new Text(day);
          }
          if (latestMessage.getType() == 0) {
            if (latestMessage.getData().length() > 15) {
              messageLabel.setText(latestMessage.getData().substring(0, 15) + "...");
            } else {
              messageLabel.setText(latestMessage.getData());
            }

          } else {//type = 6
            if (Objects.equals(latestMessage.getSentByUser(), username)) {
              messageLabel.setText("你发送了一个文件");
            } else {
              messageLabel.setText("你收到一个新文件");
            }
          }
        } else {
          messageLabel.setText(null);
          timeText.setText(null);
        }

        StackPane stackPane = new StackPane();
        Text countText = new Text("10");
        int count = 0;
        for (Message message : messages) {
          if (!message.isHaveRead()) {
            count++;
          } else {
            count = 0;
          }
        }
        if (count == 0) {
          stackPane.setVisible(false);
        } else {
          countText.setText(String.valueOf(count));
          stackPane.setVisible(true);
        }


        Circle circle = new Circle(10, Color.RED);

        countText.setStyle("-fx-fill: white; -fx-font-size: 10px; -fx-font-weight: bold ");
        stackPane.getChildren().addAll(circle, countText);

// 创建垂直布局容器vbox2，并添加时间标签、Region和圆形
        Region region = new Region();
        region.setMaxWidth(Double.MAX_VALUE);
//                region.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(region, Priority.ALWAYS);

        VBox vbox2 = new VBox();
        HBox hBoxCircle = new HBox(region, stackPane);
        hBoxCircle.setPadding(new Insets(5, 0, 0, 0));
        vbox2.getChildren().addAll(timeText, hBoxCircle);
//                vbox2.getChildren().addAll(timeText);
        vbox2.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(vbox2, Priority.ALWAYS);

        vbox2.setAlignment(Pos.TOP_RIGHT);


        HBox hboxLeft = new HBox();
        hboxLeft.getChildren().addAll(headPortrait, vbox1);
        hboxLeft.setAlignment(Pos.CENTER_LEFT);
        hboxLeft.setSpacing(10);

        hbox.setSpacing(10);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.getChildren().addAll(hboxLeft, vbox2);


        // 将头像、VBox1和未读消息数添加到HBox中

        // 设置HBox的对齐方式为靠左
        setGraphic(hbox);
        setText(null);

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
            setText(null);
            setGraphic(null);
            return;
          }
          System.out.println("getBelongToChat:" + msg.getBelongToChat() + "| Controller.this.currentChat:" + Controller.this.currentChat + "| data:" + msg.getData() + "| chatContentList.size:" + Controller.this.chatContentList.getItems().size());
          setUserData(msg.getTimestamp());
//                    if (!Objects.equals(msg.getBelongToChat(), Controller.this.currentChat)){
//                        return;
//                    }

          HBox wrapper = new HBox();
          Label nameText = new Label(msg.getSentByUser());
          Label msgLabel = new Label(msg.getData());

          nameText.setPrefSize(50, 20);
          nameText.setWrapText(true);
          nameText.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentByUser())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameText);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameText, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }

  private class ChatBubbleCell extends ListCell<Message> {
    private final Color SENT_COLOR = Color.rgb(0, 159, 230);
    private final Color RECEIVED_COLOR = Color.LIGHTGRAY;

    @Override
    protected void updateItem(Message message, boolean empty) {
      super.updateItem(message, empty);

      if (empty || message == null) {
        setGraphic(null);
      } else {
//                Label label = new Label(message.getData().replace("\n", ""));
//                label.setPadding(new Insets(10));
//                label.setMaxWidth(400);
//                label.setWrapText(true);
//                Color bubbleColor;

        HBox hbox = new HBox();
        hbox.setSpacing(10);
        hbox.setPadding(new Insets(10, 15, 10, 15));
//                hbox.setStyle("-fx-background-color: #8c6c6c");

        double AVATAR_SIZE = 40;


        InputStream isImgReceiver = null;
        InputStream isImgSender = null;
        try {
          isImgReceiver = loadImg(message.getSentByUser());
          isImgSender = loadImg(Controller.this.username);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        ;

        ImageView avatar = new ImageView(message.getSentByUser().equals(Controller.this.username) ?
                new Image(isImgSender, AVATAR_SIZE, AVATAR_SIZE, true, true) :
                new Image(isImgReceiver, AVATAR_SIZE, AVATAR_SIZE, true, true));
        avatar.setFitWidth(AVATAR_SIZE);
        avatar.setFitHeight(AVATAR_SIZE);
        avatar.setClip(new Circle(AVATAR_SIZE / 2, AVATAR_SIZE / 2, AVATAR_SIZE / 2));

        VBox avatarBox = new VBox(avatar);
        avatarBox.setAlignment(Pos.TOP_LEFT);
        Label label;

        if (message.getType() == 0) {
          label = new Label(message.getData());
        } else {//type == 6
          label = new Label(message.getData().substring(message.getData().indexOf(":") + 1));
          label.setStyle("-fx-font-style: italic; -fx-underline: true;");
        }


        label.setPadding(new Insets(10));
        label.setMaxWidth(400);
        label.setWrapText(true);
        Color bubbleColor;
        VBox contentBox = new VBox(label);
        contentBox.setAlignment(Pos.TOP_LEFT);

        if (message.getSentByUser().equals(Controller.this.username)) {
          bubbleColor = SENT_COLOR;
          label.setStyle(label.getStyle() + "-fx-text-fill: white;-fx-font-size: 16px");
          hbox.getChildren().addAll(contentBox, avatarBox);
          hbox.setAlignment(Pos.CENTER_RIGHT);
        } else {
          bubbleColor = RECEIVED_COLOR;
          label.setStyle(label.getStyle() + "-fx-text-fill: black;-fx-font-size: 16px");
          hbox.getChildren().addAll(avatarBox, contentBox);
          hbox.setAlignment(Pos.CENTER_LEFT);
        }

        label.setBackground(new Background(new BackgroundFill(
                bubbleColor, new CornerRadii(5), Insets.EMPTY)));

        hbox.setMaxWidth(Region.USE_COMPUTED_SIZE);

        setGraphic(hbox);
      }
    }
  }

  private class clientSocket implements Runnable {

    private final Socket client;
    private final ObjectInputStream in;

    private final Set<String> userSet;

    public clientSocket(Socket client, Set<String> userSet) throws IOException {
      this.client = client;
      in = new ObjectInputStream(client.getInputStream());
      this.userSet = userSet;
    }

    public void run() {

      try {
        while (!Thread.currentThread().isInterrupted()) {
          Message message = (Message) in.readObject();
          System.out.println(message);
          if (message.getType() == -1) {//注册成功返回消息
            Platform.runLater(() -> {
              Alert alert = new Alert(Alert.AlertType.INFORMATION);
              alert.setContentText(message.getData() + "go back to login");
              alert.showAndWait();
              loginController.backToLogin();
            });

            String path = "allUser/" + message.getSendToUser();
            File dir = new File(path);
            if (!dir.exists()) {
              dir.mkdirs();
              System.out.println("Folder created: " + dir.getAbsolutePath());
            } else {
              System.out.println("Folder already exists: " + dir.getAbsolutePath());
            }

          } else if (message.getType() == -2) {//登陆成功返回消息
            synchronized (Controller.this.userSet) {
              Platform.runLater(Controller.this::closeLoginStage);
//                            Platform.runLater(()->{
//                                Controller.this.chatList.refresh();
//                                Controller.this.chatContentList.refresh();
//                            });

            }
          } else if (message.getType() == -3) {
            synchronized (Controller.this) {
              Controller.this.chatList.refresh();
              Controller.this.chatContentList.refresh();
            }
          } else if (message.getType() == -4) {
            Thread.currentThread().interrupt();
            client.close();
            Controller.this.out.close();
            in.close();
          } else if (message.getType() == -5) {
            synchronized (Controller.this.userSet) {
              Controller.this.userSet.clear();
              Controller.this.userSet.addAll(Arrays.asList(message.getData().split(",")));
              Platform.runLater(() -> Controller.this.currentOnlineCnt.textProperty().set("OnlineCnt: " + Controller.this.userSet.size()));
              Controller.this.userSet.notifyAll();
            }
          } else if (message.getType() == 404) {//登陆失败返回消息
            Platform.runLater(() -> {
              Alert alert = new Alert(Alert.AlertType.WARNING);
              alert.setContentText(message.getData());
              alert.showAndWait();
              System.out.println(message.getData());
              loginController.backToLogin();
            });
          } else if (message.getType() == 505) {//注册失败返回消息
            Platform.runLater(() -> {
              Alert alert = new Alert(Alert.AlertType.WARNING);
              alert.setContentText(message.getData());
              alert.showAndWait();
              System.out.println(message.getData());
              loginController.clearAllField();
            });
          } else if (message.getType() == 0 || message.getType() == 6) {

            Platform.runLater(() -> {
              ObservableList<String> items = Controller.this.chatList.getItems();
              int currentChatIndex = items.indexOf(currentChat);
              int receiveChatIndex = Integer.MAX_VALUE;
              if (Controller.this.chatList.getItems().contains(message.getBelongToChat())) {//新发来的消息放在listview最前面
                receiveChatIndex = items.indexOf(message.getBelongToChat());
                items.remove(message.getBelongToChat());
              }
//              System.out.println("currentChatIndex:" + currentChatIndex + "receiveChatIndex:" + receiveChatIndex);

              items.add(0, message.getBelongToChat());
              if (currentChatIndex < receiveChatIndex) {
                chatList.getSelectionModel().select(currentChatIndex + 1);
              } else {
                chatList.getSelectionModel().select(currentChatIndex);
              }
              Controller.this.chatList.setItems(items);
              Controller.this.chatList.refresh();
//                            System.out.println(Controller.this.chatList.getItems());
            });


            synchronized (messageItems) {
//                            System.out.println("receive message, update listview");
              Platform.runLater(() -> {
                messageItems.add(message);
                ObservableList<Message> filterMessageItems = FXCollections.observableArrayList(
                        messageItems.stream().filter(e -> e.getBelongToChat().equals(currentChat))
                                .collect(Collectors.toList()));
                chatContentList.setItems(filterMessageItems);
//                                chatContentList.getSelectionModel().selectLast();
              });
            }
          }
        }


//                String line;
//                while ((line = in.readLine()) != null) {
//                    String finalLine = line;
//
//                }
      } catch (SocketException e) {
        Platform.runLater(() -> {
          Stage stage = new Stage();
          HBox box = new HBox(50);
          box.setAlignment(Pos.CENTER);
          box.setPadding(new Insets(20, 20, 20, 20));
          Label label = new Label("服务器异常关闭，客户端即将强制退出！");
          box.getChildren().addAll(label);
          stage.setScene(new Scene(box, 300, 130));
          stage.initModality(Modality.APPLICATION_MODAL);
          stage.setTitle("Error");
          stage.showAndWait();
          System.exit(0);
        });


      } catch (IOException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public InputStream loadImg(String username) throws IOException {
    Path headJpgPath = Paths.get("allUser/" + username + "/head.jpg");
    Path headPngPath = Paths.get("allUser/" + username + "/head.png");
    InputStream isImg;
    if (Files.exists(headJpgPath)) {
      isImg = Files.newInputStream(headJpgPath);
//            if (isImg == null){
//                isImg = Files.newInputStream(new File(headJpgPath.toString()).toPath());
//            }
//            System.out.println("diy jpg exist");

    } else if (Files.exists(headPngPath)) {
//            System.out.println("diy png exist");
      isImg = Files.newInputStream(headPngPath);

//            isImg = Files.newInputStream(new File(headJpgPath.toString()).toPath());
    } else {
//            System.out.println(headPngPath+" not exist");
//            System.out.println(headJpgPath+" not exist");
      isImg = Files.newInputStream(Paths.get("allUser/default.png"));
    }
    return isImg;
  }


}






