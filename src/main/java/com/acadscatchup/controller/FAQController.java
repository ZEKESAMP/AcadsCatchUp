package com.acadscatchup.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the Project FAQ and Credits dialog.
 * Supports window dragging when used as an undecorated dialog.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class FAQController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private HBox headerBar;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        if (headerBar != null) {
            headerBar.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            headerBar.setOnMouseDragged(event -> {
                Stage stage = (Stage) headerBar.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Node source = (Node) event.getSource();
        com.acadscatchup.util.ModalOverlay.close(source);
    }
}
