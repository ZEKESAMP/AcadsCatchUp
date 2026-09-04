package com.acadscatchup.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Universal Password Visibility Toggle Helper.
 * Provides modern eye-icon toggle buttons and "Show Password" checkboxes
 * for password input fields across Login, Forgot Password, Change Password,
 * and User Management forms.
 *
 * Built with vector SVG graphics for 100% crisp, cross-platform fidelity
 * across Windows, Linux, and macOS without relying on OS-specific emoji fonts.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class PasswordToggleHelper {

    public static final String DEVELOPER = "F4TAL";

    // Standard 24x24 Material Design Eye Vector Paths
    private static final String SVG_EYE_OPEN =
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5z" +
            "M12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";

    private static final String SVG_EYE_SLASH =
            "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.44-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7z" +
            "M2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27z" +
            "M7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2z" +
            "m4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    private static final Color COLOR_MUTED = Color.web("#94a3b8");
    private static final Color COLOR_ACTIVE = Color.web("#38bdf8");

    /**
     * Create an SVG eye icon scaled to the specified factor.
     */
    public static SVGPath createEyeIcon(boolean visible, double scale) {
        SVGPath path = new SVGPath();
        path.setContent(visible ? SVG_EYE_SLASH : SVG_EYE_OPEN);
        path.setFill(visible ? COLOR_ACTIVE : COLOR_MUTED);
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    /**
     * Self-contained PasswordBox component containing a PasswordField,
     * companion TextField, and right-aligned eye toggle button.
     */
    public static class PasswordBox extends StackPane {
        public static final String DEVELOPER = "F4TAL";
        private final PasswordField passwordField;
        private final TextField plainTextField;
        private final Button eyeButton;
        private final BooleanProperty passwordVisible = new SimpleBooleanProperty(false);

        public PasswordBox(String promptText, String style) {
            this.passwordField = new PasswordField();
            this.plainTextField = new TextField();
            this.eyeButton = new Button();

            passwordField.setPromptText(promptText);
            plainTextField.setPromptText(promptText);

            if (style != null && !style.isEmpty()) {
                passwordField.setStyle(style);
                plainTextField.setStyle(style);
            } else {
                passwordField.getStyleClass().addAll("text-input", "password-input-field");
                plainTextField.getStyleClass().addAll("text-input", "password-input-field");
            }

            plainTextField.setVisible(false);
            plainTextField.setManaged(false);

            // Bind text bidirectionally
            passwordField.textProperty().bindBidirectional(plainTextField.textProperty());

            // Eye Button Setup
            eyeButton.getStyleClass().add("btn-eye-toggle");
            eyeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6; -fx-background-radius: 4;");
            eyeButton.setFocusTraversable(false);
            updateEyeButton(false);

            eyeButton.setOnAction(e -> setPasswordVisible(!isPasswordVisible()));

            // Hover effects on eye icon
            eyeButton.setOnMouseEntered(e -> {
                SVGPath path = (SVGPath) eyeButton.getGraphic();
                if (path != null) path.setFill(COLOR_ACTIVE);
            });
            eyeButton.setOnMouseExited(e -> {
                SVGPath path = (SVGPath) eyeButton.getGraphic();
                if (path != null) {
                    path.setFill(isPasswordVisible() ? COLOR_ACTIVE : COLOR_MUTED);
                }
            });

            passwordVisible.addListener((obs, oldVal, newVal) -> updateVisibility(newVal));

            // Sync disabled state
            disableProperty().addListener((obs, oldV, disabled) -> {
                passwordField.setDisable(disabled);
                plainTextField.setDisable(disabled);
                eyeButton.setDisable(disabled);
            });

            // Layout inside StackPane
            HBox.setHgrow(this, Priority.ALWAYS);
            setAlignment(Pos.CENTER_RIGHT);
            getChildren().addAll(passwordField, plainTextField, eyeButton);
            StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);
            StackPane.setMargin(eyeButton, new Insets(0, 6, 0, 0));
        }

        private void updateEyeButton(boolean visible) {
            eyeButton.setGraphic(createEyeIcon(visible, 0.72));
            Tooltip.install(eyeButton, new Tooltip(visible ? "Hide Password" : "Show Password"));
        }

        private void updateVisibility(boolean show) {
            updateEyeButton(show);
            if (show) {
                plainTextField.setVisible(true);
                plainTextField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                if (passwordField.isFocused()) {
                    plainTextField.requestFocus();
                    plainTextField.positionCaret(plainTextField.getText().length());
                }
            } else {
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                plainTextField.setVisible(false);
                plainTextField.setManaged(false);
                if (plainTextField.isFocused()) {
                    passwordField.requestFocus();
                    passwordField.positionCaret(passwordField.getText().length());
                }
            }
        }

        @Override
        public void requestFocus() {
            if (isPasswordVisible()) {
                plainTextField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
        }

        public String getText() {
            return isPasswordVisible() ? plainTextField.getText() : passwordField.getText();
        }

        public void setText(String text) {
            passwordField.setText(text);
        }

        public void clear() {
            passwordField.clear();
        }

        public void setOnAction(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
            passwordField.setOnAction(handler);
            plainTextField.setOnAction(handler);
        }

        public PasswordField getPasswordField() {
            return passwordField;
        }

        public TextField getPlainTextField() {
            return plainTextField;
        }

        public Button getEyeButton() {
            return eyeButton;
        }

        public BooleanProperty passwordVisibleProperty() {
            return passwordVisible;
        }

        public boolean isPasswordVisible() {
            return passwordVisible.get();
        }

        public void setPasswordVisible(boolean visible) {
            passwordVisible.set(visible);
        }

        /**
         * Create a dedicated "Show Password" CheckBox with eye icon graphic
         * bound directly to this PasswordBox.
         */
        public CheckBox createShowPasswordCheckBox(String labelText) {
            CheckBox cb = new CheckBox(labelText != null ? labelText : "Show Password");
            cb.getStyleClass().add("check-show-password");
            cb.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-cursor: hand;");
            cb.setGraphic(createEyeIcon(isPasswordVisible(), 0.60));

            cb.selectedProperty().bindBidirectional(passwordVisible);
            cb.selectedProperty().addListener((obs, oldV, isSel) -> {
                cb.setGraphic(createEyeIcon(isSel, 0.60));
                cb.setStyle(isSel
                        ? "-fx-text-fill: #38bdf8; -fx-font-size: 11.5px; -fx-cursor: hand; -fx-font-weight: bold;"
                        : "-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-cursor: hand;");
            });

            return cb;
        }
    }

    /**
     * Factory method to create a new PasswordBox.
     */
    public static PasswordBox createPasswordBox(String promptText, String style) {
        return new PasswordBox(promptText, style);
    }

    /**
     * Set up and bind visibility controls for existing FXML nodes.
     */
    public static void setupExisting(
            PasswordField passwordField,
            TextField plainTextField,
            Button eyeButton
    ) {
        setupExisting(passwordField, plainTextField, eyeButton, null);
    }

    /**
     * Set up and bind visibility controls for existing FXML nodes.
     */
    public static void setupExisting(
            PasswordField passwordField,
            TextField plainTextField,
            Button eyeButton,
            CheckBox showPasswordCheck
    ) {
        if (!passwordField.getStyleClass().contains("password-input-field")) {
            passwordField.getStyleClass().add("password-input-field");
        }
        if (plainTextField != null && !plainTextField.getStyleClass().contains("password-input-field")) {
            plainTextField.getStyleClass().add("password-input-field");
        }

        BooleanProperty visibleProp = new SimpleBooleanProperty(false);

        plainTextField.setVisible(false);
        plainTextField.setManaged(false);

        // Bidirectional text binding
        passwordField.textProperty().bindBidirectional(plainTextField.textProperty());

        // Eye Button setup
        if (eyeButton != null) {
            StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);
            StackPane.setMargin(eyeButton, new Insets(0, 6, 0, 0));
            eyeButton.setFocusTraversable(false);
            eyeButton.setGraphic(createEyeIcon(false, 0.72));
            Tooltip.install(eyeButton, new Tooltip("Show Password"));

            eyeButton.setOnAction(e -> visibleProp.set(!visibleProp.get()));

            eyeButton.setOnMouseEntered(e -> {
                SVGPath p = (SVGPath) eyeButton.getGraphic();
                if (p != null) p.setFill(COLOR_ACTIVE);
            });
            eyeButton.setOnMouseExited(e -> {
                SVGPath p = (SVGPath) eyeButton.getGraphic();
                if (p != null) p.setFill(visibleProp.get() ? COLOR_ACTIVE : COLOR_MUTED);
            });
        }

        // Show Password CheckBox setup
        if (showPasswordCheck != null) {
            showPasswordCheck.setGraphic(createEyeIcon(false, 0.60));
            showPasswordCheck.selectedProperty().bindBidirectional(visibleProp);
            showPasswordCheck.selectedProperty().addListener((obs, oldV, isSel) -> {
                showPasswordCheck.setGraphic(createEyeIcon(isSel, 0.60));
                showPasswordCheck.setStyle(isSel
                        ? "-fx-text-fill: #38bdf8; -fx-font-size: 11.5px; -fx-cursor: hand; -fx-font-weight: bold;"
                        : "-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-cursor: hand;");
            });
        }

        // Visibility listener
        visibleProp.addListener((obs, oldVal, show) -> {
            if (eyeButton != null) {
                eyeButton.setGraphic(createEyeIcon(show, 0.72));
                Tooltip.install(eyeButton, new Tooltip(show ? "Hide Password" : "Show Password"));
            }

            if (show) {
                plainTextField.setVisible(true);
                plainTextField.setManaged(true);
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                if (passwordField.isFocused()) {
                    plainTextField.requestFocus();
                    plainTextField.positionCaret(plainTextField.getText().length());
                }
            } else {
                passwordField.setVisible(true);
                passwordField.setManaged(true);
                plainTextField.setVisible(false);
                plainTextField.setManaged(false);
                if (plainTextField.isFocused()) {
                    passwordField.requestFocus();
                    passwordField.positionCaret(passwordField.getText().length());
                }
            }
        });
    }

    /**
     * Create a master "Show Password" toggle checkbox that controls multiple
     * PasswordBoxes at once (e.g. for New Password + Confirm Password).
     */
    public static CheckBox createMasterToggle(String labelText, PasswordBox... boxes) {
        CheckBox cb = new CheckBox(labelText != null ? labelText : "Show Password");
        cb.getStyleClass().add("check-show-password");
        cb.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-cursor: hand;");
        cb.setGraphic(createEyeIcon(false, 0.60));

        cb.selectedProperty().addListener((obs, oldV, isSel) -> {
            cb.setGraphic(createEyeIcon(isSel, 0.60));
            cb.setStyle(isSel
                    ? "-fx-text-fill: #38bdf8; -fx-font-size: 11.5px; -fx-cursor: hand; -fx-font-weight: bold;"
                    : "-fx-text-fill: #94a3b8; -fx-font-size: 11.5px; -fx-cursor: hand;");

            for (PasswordBox box : boxes) {
                if (box != null && box.isPasswordVisible() != isSel) {
                    box.setPasswordVisible(isSel);
                }
            }
        });

        // If any box is toggled individually, keep master checkbox in sync if all are visible
        for (PasswordBox box : boxes) {
            if (box != null) {
                box.passwordVisibleProperty().addListener((obs, o, isVis) -> {
                    boolean allVis = true;
                    for (PasswordBox b : boxes) {
                        if (b != null && !b.isPasswordVisible()) {
                            allVis = false;
                            break;
                        }
                    }
                    if (cb.isSelected() != allVis) {
                        cb.setSelected(allVis);
                    }
                });
            }
        }

        return cb;
    }

    /**
     * Helper to read the current password string regardless of which field is active.
     */
    public static String getText(PasswordField pf, TextField tf) {
        if (tf != null && tf.isVisible()) {
            return tf.getText();
        }
        return pf != null ? pf.getText() : "";
    }
}
