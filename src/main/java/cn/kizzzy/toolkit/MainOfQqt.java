package cn.kizzzy.toolkit;

import cn.kizzzy.toolkit.controller.Controllers;
import cn.kizzzy.toolkit.controller.QqtLocalController;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainOfQqt extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Controllers.start(null, primaryStage, QqtLocalController.class);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
