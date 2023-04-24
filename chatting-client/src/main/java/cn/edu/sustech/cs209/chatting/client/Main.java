package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {

//        FXMLLoader mainFxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
//        stage.setScene(new Scene(mainFxmlLoader.load()));
//        stage.setTitle("Chatting Client");
//        stage.setResizable(false);
//        stage.show();
//
//        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login.fxml"));
//        Parent root = loginLoader.load();
//        LoginController loginController = loginLoader.getController();
//        Stage loginStage = new Stage();
//        loginStage.setScene(new Scene(root));
//        loginStage.showAndWait();


        //创建登陆注册界面Scene
//        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("login.fxml"));
//        Parent loginRoot = loginLoader.load();
//        Scene loginScene = new Scene(loginRoot);
//        stage.setScene(loginScene);
//        stage.showAndWait();


        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("main.fxml"));

        mainLoader.setControllerFactory(param -> {
            Controller controller = new Controller();
            controller.stage =stage;
            return controller;
        });
        Parent mainRoot = mainLoader.load();
        Scene mainScene = new Scene(mainRoot,1000,600);
        stage.setScene(mainScene);



        stage.setTitle("Chatting Client");
        stage.setResizable(false);


    }
}
