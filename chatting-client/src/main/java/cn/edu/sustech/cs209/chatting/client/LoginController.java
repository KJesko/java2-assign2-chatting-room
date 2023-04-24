package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Set;

public class LoginController implements Initializable {

    Stage loginStage;

    Controller controller;

    public void setLoginStage(Stage loginStage) {
        this.loginStage = loginStage;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private VBox registerBox;

    @FXML
    private VBox loginBox;

    @FXML
    private TextField registerUsernameField;

    @FXML
    private PasswordField registerRepeatPasswordField;

    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private Button submitButton;

    @FXML
    private Button backToLoginButton;

    //
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 在这里可以添加初始化操作，例如设置按钮的事件监听器等
        // ...
        loginButton.setStyle("-fx-background-color: rgb(0,159,230); -fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // 设置鼠标悬停时按钮的样式
        loginButton.setOnMouseEntered(event -> {
            loginButton.setStyle("-fx-background-color: rgb(0,154,255); -fx-border-color: transparent; -fx-text-fill: white; -fx-font-size: 24px;-fx-font-weight: bold;");
        });

        // 设置鼠标离开时按钮的样式
        loginButton.setOnMouseExited(event -> {
            loginButton.setStyle("-fx-background-color: rgb(0,159,230); -fx-border-color: transparent; -fx-text-fill: white; -fx-font-size: 24px;-fx-font-weight: bold;");
        });


        registerButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: blue;"); // 设置按钮的样式，包括透明背景、无边框、文字颜色为蓝色

        // 设置鼠标悬停时按钮的样式
        registerButton.setOnMouseEntered(event -> {
//            registerButton.setStyle("-fx-background-color: lightgray; -fx-border-color: transparent; -fx-text-fill: blue;");
            registerButton.setStyle("-fx-border-width: 0 0 1 0; -fx-border-color: transparent transparent black transparent; -fx-background-color: lightgray; -fx-text-fill: blue;");
        });

        // 设置鼠标离开时按钮的样式
        registerButton.setOnMouseExited(event -> {
            registerButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: blue;");
        });

        usernameField.setOnMouseClicked(e -> {
            usernameField.setStyle(usernameField.getStyle() + "-fx-border-color: transparent transparent rgb(0, 159, 230) transparent;");

        });
        setFieldFocusState(passwordField);
        setFieldFocusState(usernameField);
        setFieldFocusState(registerPasswordField);
        setFieldFocusState(registerRepeatPasswordField);
        setFieldFocusState(registerUsernameField);

    }

    private void setFieldFocusState(PasswordField passwordField) {

        passwordField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // 当TextField被选中时执行的逻辑
                passwordField.setStyle(passwordField.getStyle() + "-fx-border-color: transparent transparent rgb(0, 159, 230) transparent;-fx-font-size: 16px;");
                passwordField.setPromptText(null);
            } else {
                // 当TextField失去焦点时执行的逻辑
                passwordField.setStyle(passwordField.getStyle() + "-fx-border-color: transparent transparent gray transparent;");

            }
        });
    }

    private void setFieldFocusState(TextField textField) {

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // 当TextField被选中时执行的逻辑
                textField.setStyle(textField.getStyle() + "-fx-border-color: transparent transparent rgb(0, 159, 230) transparent; -fx-font-size: 16px;");
                textField.setPromptText(null);
            } else {
                // 当TextField失去焦点时执行的逻辑
                textField.setStyle(textField.getStyle() + "-fx-border-color: transparent transparent gray transparent;");

            }
        });
    }

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean flag = true;
        if (username == null || username.isEmpty()){
            usernameField.setText(null);
            usernameField.setStyle("-fx-font-size: 12px;");
            usernameField.setPromptText("username can't be empty");
            flag = false;
        }
        else if (username.contains(",")){
            usernameField.setText(null);
            usernameField.setStyle("-fx-font-size: 12px;");
            usernameField.setPromptText("Commas are not allowed in username");
            flag = false;
        }

        if(password == null || password.isEmpty()) {
            passwordField.setText(null);
            passwordField.setStyle("-fx-font-size: 12px;");
            passwordField.setPromptText("password can't be empty");
            flag = false;

        }


        if (flag){
            this.controller.username = username;
            this.controller.pwd = password;

            Socket client = this.controller.client;

            String sentByUser = username;
            String sentToUser = "server";
            String sentByUrl = client.getLocalSocketAddress().toString();
            String sentToUrl = client.getRemoteSocketAddress().toString();
            String data = username + "," + password;
            Long timestamp = System.currentTimeMillis();
            Message message = new Message(2, timestamp, sentByUser, sentToUser, sentByUrl, sentToUrl, data);
            controller.out.writeObject(message);
        }

        // 在这里编写登录逻辑，例如检查用户名和密码是否正确等
        // ...
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        registerBox.setVisible(true);
        loginBox.setVisible(false);
    }

    @FXML
    private void handleRegisterSubmit(ActionEvent event) throws IOException {
        String regUsername = registerUsernameField.getText();
        String regPassword1 = registerPasswordField.getText();
        String regPassword2 = registerRepeatPasswordField.getText();
        boolean flag = true;

        if (regUsername == null|| regUsername.isEmpty()){
            registerUsernameField.setText(null);
            registerUsernameField.setStyle("-fx-font-size: 12px;");
            registerUsernameField.setPromptText("username can't be empty");
            flag = false;

        }else if (regUsername.contains(",")){
            registerUsernameField.setText(null);
            registerUsernameField.setStyle("-fx-font-size: 12px;");
            registerUsernameField.setPromptText("Commas are not allowed in username");
            flag = false;

        }


        if(regPassword1 == null || regPassword1.isEmpty()){
            registerPasswordField.setText(null);
            registerPasswordField.setStyle("-fx-font-size: 12px;");
            registerPasswordField.setPromptText("password can't be empty");
            flag = false;
        }
        if (regPassword2 == null  || regPassword2.isEmpty())  {
            registerRepeatPasswordField.setText(null);
            registerRepeatPasswordField.setStyle("-fx-font-size: 12px;");
            registerRepeatPasswordField.setPromptText("repeat password can't be empty");
            flag = false;
        }

        if (regPassword1!= null && regPassword2 !=null && !regPassword1.equals(regPassword2)) {
            registerRepeatPasswordField.setText(null);
            registerRepeatPasswordField.setStyle("-fx-font-size: 12px;");
            registerRepeatPasswordField.setPromptText("Two password must be same");
            flag = false;
        }
        if (flag){
            Socket client = this.controller.client;

            String sentByUser = regUsername;
            String sentToUser = "server";
            String sentByUrl = client.getLocalSocketAddress().toString();
            String sentToUrl = client.getRemoteSocketAddress().toString();
            String data = regUsername + "," + regPassword1;
            Long timestamp = System.currentTimeMillis();
            Message message = new Message(1, timestamp, sentByUser, sentToUser, sentByUrl, sentToUrl, data);
            controller.out.writeObject(message);
            // 在这里编写注册逻辑，例如检查用户名和密码是否符合要求等
            // ...
            // 注册完成后返回登录界面
            backToLogin();
        }


    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        backToLogin();
    }

    // 辅助方法：返回登录界面
    public void backToLogin() {
        registerBox.setVisible(false);
        loginBox.setVisible(true);
        clearAllField();
    }
    public void clearAllField() {
        usernameField.setText("");
        passwordField.setText("");
        registerUsernameField.setText("");
        registerPasswordField.setText("");
        registerRepeatPasswordField.setText("");
        usernameField.setPromptText(null);
        passwordField.setPromptText(null);
        registerUsernameField.setPromptText(null);
        registerPasswordField.setPromptText(null);
        registerRepeatPasswordField.setPromptText(null);
        usernameField.setStyle("-fx-font-size: 12px;");
        passwordField.setStyle("-fx-font-size: 12px;");
        registerUsernameField.setStyle("-fx-font-size: 12px;");
        registerPasswordField.setStyle("-fx-font-size: 12px;");
        registerRepeatPasswordField.setStyle("-fx-font-size: 12px;");
    }

    public void handleRegisterSuccess(Message message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message.getData() + "go back to login");
        alert.showAndWait();
        this.backToLogin();
    }
    public void handleRegisterFail(Message message){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(message.getData());
        alert.showAndWait();
        System.out.println(message.getData());
        this.clearAllField();
    }

    public void handleLoginSuccess(Message message){
        loginStage.close();
    }

    public void handleLoginFail(Message message){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(message.getData());
        alert.showAndWait();
        System.out.println(message.getData());
        this.backToLogin();
    }


}
