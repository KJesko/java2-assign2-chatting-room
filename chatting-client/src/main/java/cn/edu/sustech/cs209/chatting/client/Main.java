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


    FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("main.fxml"));

    mainLoader.setControllerFactory(param -> {
      Controller controller = new Controller();
      controller.stage = stage;
      return controller;
    });
    Parent mainRoot = mainLoader.load();
    Scene mainScene = new Scene(mainRoot, 1000, 600);
    stage.setScene(mainScene);


    stage.setTitle("Chatting Client");
    stage.setResizable(false);


  }
}
