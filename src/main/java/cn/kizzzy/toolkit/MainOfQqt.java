package cn.kizzzy.toolkit;

import cn.kizzzy.javafx.plugin.PluginView;
import cn.kizzzy.toolkit.controller.QqtLocalController;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainOfQqt extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        PluginView.show(null, primaryStage, QqtLocalController.class);
    }
}
