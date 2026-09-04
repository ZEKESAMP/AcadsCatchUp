package com.acadscatchup;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * AcadsCatchUp — Application Entry Point.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class Main extends Application {

    public static final String DEVELOPER     = "F4TAL";
    public static final String CREATOR       = "Stevenson James G. Gastanes (F4TAL)";
    public static final String APP_NAME       = "AcadsCatchUp";
    public static final String BUILD_VERSION  = "1.0.1-PROD-F4TAL";
    public static final String SIGNATURE      = "AcadsCatchUp • Engineered by F4TAL";

    @Override
    public void init() throws Exception {
        // Initialize cross-platform OS compatibility (emoji fallback, font detection)
        com.acadscatchup.util.OSCompat.init();

        System.out.println("[F4TAL Engine] Starting " + APP_NAME + " [" + BUILD_VERSION + "]");
        // Initialize SQLite DB and seed demo/admin accounts on launch
        new Thread(() -> {
            try {
                com.acadscatchup.db.DBConnection.getConnection();
            } catch (Exception e) {
                System.err.println("[F4TAL DB Error] Database init error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/acadscatchup/fxml/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);

        primaryStage.setTitle("AcadsCatchUp — Login");
        try {
            primaryStage.getIcons().add(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/com/acadscatchup/img/book_icon_blue.png")));
        } catch (Exception ignored) {}
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(580);
        primaryStage.show();
        com.acadscatchup.util.WindowUtil.initFullScreenWithCentering(primaryStage, 540, 720);

        // Discord-style System Tray
        com.acadscatchup.util.AppTrayManager.initTray(primaryStage);
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            com.acadscatchup.util.AppTrayManager.handleCloseRequest(primaryStage);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
