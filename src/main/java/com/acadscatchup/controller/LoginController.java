package com.acadscatchup.controller;

import com.acadscatchup.dao.UserDAO;
import com.acadscatchup.model.User;
import com.acadscatchup.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Optional;

/**
 * Handles login screen actions.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class LoginController {

    public static final String DEVELOPER = "F4TAL";

    @FXML private StackPane     rootPane;
    @FXML private ImageView     bgImageView;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     plainPasswordField;
    @FXML private Button        btnTogglePassword;
    @FXML private Label         errorLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = com.acadscatchup.util.PasswordToggleHelper.getText(passwordField, plainPasswordField);

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password.");
            return;
        }

        if (!com.acadscatchup.db.DBConnection.hasInternet(true)) {
            javafx.stage.Window win = (rootPane.getScene() != null) ? rootPane.getScene().getWindow() : null;
            com.acadscatchup.util.CustomAlert.showError(
                    win,
                    "No Internet Connection",
                    "⚠️ No Internet Connection Detected\n\nAcadsCatchUp requires an active internet connection to access the central online database.\n\nPlease check your network connection and try again."
            );
            return;
        }

        // Disable inputs and show loading overlay
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        if (plainPasswordField != null) plainPasswordField.setDisable(true);
        if (btnTogglePassword != null) btnTogglePassword.setDisable(true);
        errorLabel.setText("");

        com.acadscatchup.util.LoadingOverlay.runWithResult(
                rootPane,
                "Authenticating...",
                () -> {
                    try {
                        return userDAO.login(username, password);
                    } catch (Exception e) {
                        // Store error message for UI thread
                        javafx.application.Platform.runLater(() -> {
                            errorLabel.setText("Online Database Connection Error: " + e.getMessage());
                            errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 11px;");
                        });
                        return null;
                    }
                },
                user -> {
                    usernameField.setDisable(false);
                    passwordField.setDisable(false);
                    if (plainPasswordField != null) plainPasswordField.setDisable(false);
                    if (btnTogglePassword != null) btnTogglePassword.setDisable(false);

                    if (user == null) {
                        if (errorLabel.getText().isEmpty()) {
                            errorLabel.setText("Invalid username or password. Please try again.");
                        }
                        passwordField.clear();
                        if (plainPasswordField != null) plainPasswordField.clear();
                        return;
                    }

                    // First-Time Login Email Verification Check
                    if (!user.isAdmin() && !user.isVerified()) {
                        Window win = (rootPane.getScene() != null) ? rootPane.getScene().getWindow() : null;
                        String targetEmail = user.getEmail();

                        if (targetEmail == null || targetEmail.isBlank()) {
                            final String[] emailResult = new String[]{null};
                            final boolean[] skipVerification = new boolean[]{false};
                            VBox promptBox = new VBox(14);
                            promptBox.setStyle("-fx-background-color: #1a1d2e; -fx-padding: 24; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

                            Label title = new Label(com.acadscatchup.util.OSCompat.label("Email Verification Required"));
                            title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold;");
                            Label desc = new Label("First-Time Verification for " + user.getFullName() + "\nPlease enter your Gmail address to verify your account:");
                            desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

                            TextField tf = new TextField();
                            tf.setPromptText("yourname@gmail.com");
                            tf.setStyle("-fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 8 12;");

                            HBox buttons = new HBox(10);
                            buttons.setAlignment(Pos.CENTER_RIGHT);
                            Button btnCancel = new Button("Cancel");
                            btnCancel.getStyleClass().add("btn-ghost");
                            btnCancel.setOnAction(e -> com.acadscatchup.util.ModalOverlay.close(btnCancel));

                            Button btnVerifyLater = new Button("Verify Later");
                            btnVerifyLater.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-border-color: #f59e0b; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 7 14; -fx-cursor: hand;");
                            btnVerifyLater.setOnAction(e -> {
                                skipVerification[0] = true;
                                com.acadscatchup.util.ModalOverlay.close(btnVerifyLater);
                            });

                            Button btnOk = new Button(com.acadscatchup.util.OSCompat.label("Proceed to Verification ➔"));
                            btnOk.getStyleClass().add("btn-primary");

                            Runnable submitAction = () -> {
                                String candidate = tf.getText().trim();
                                com.acadscatchup.util.GmailLookupUtil.ValidationResult res =
                                        com.acadscatchup.util.GmailLookupUtil.validateEmail(candidate);
                                if (!res.isValid()) {
                                    desc.setText("⚠️ " + res.getMessage());
                                    desc.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                                    return;
                                }
                                emailResult[0] = candidate;
                                com.acadscatchup.util.ModalOverlay.close(btnOk);
                            };
                            btnOk.setOnAction(e -> submitAction.run());
                            tf.setOnAction(e -> submitAction.run());
                            buttons.getChildren().addAll(btnCancel, btnVerifyLater, btnOk);

                            promptBox.getChildren().addAll(title, desc, tf, buttons);
                            com.acadscatchup.util.ModalOverlay.showAndWait(rootPane, promptBox, 480, 240);

                            // User chose "Verify Later" — skip verification, go to dashboard
                            if (skipVerification[0]) {
                                Session.setCurrentUser(user);
                                navigateToDashboard(user);
                                return;
                            }

                            if (emailResult[0] == null || emailResult[0].isBlank()) {
                                errorLabel.setText("Account verification is required on your first login.");
                                return;
                            }
                            targetEmail = emailResult[0];
                        }

                        boolean verified = com.acadscatchup.util.OtpVerifyDialog.show(
                                win, targetEmail, user.getFullName(), "First-Time Account Verification");
                        if (!verified) {
                            // Offer "Verify Later" as a second chance
                            boolean skipNow = com.acadscatchup.util.CustomAlert.showConfirmation(
                                    win,
                                    "Skip Verification?",
                                    "You can verify your email later. You will be prompted again on your next login.\n\nDo you want to skip verification and proceed to the dashboard?");
                            if (skipNow) {
                                // Save email if entered but skip marking as verified
                                if (targetEmail != null && !targetEmail.isBlank()) {
                                    userDAO.updateUserEmail(user.getId(), targetEmail);
                                    user.setEmail(targetEmail);
                                }
                                Session.setCurrentUser(user);
                                navigateToDashboard(user);
                                return;
                            }
                            errorLabel.setText("Account verification was not completed. Please verify to access your dashboard.");
                            return;
                        }

                        // Mark verified in DB and memory
                        userDAO.updateUserEmail(user.getId(), targetEmail);
                        userDAO.markUserVerified(user.getId());
                        user.setEmail(targetEmail);
                        user.setVerified(true);
                    } else if (com.acadscatchup.util.EmailService.is2FAEnabled() && user.getEmail() != null && !user.getEmail().isBlank()) {
                        Window win = (rootPane.getScene() != null) ? rootPane.getScene().getWindow() : null;
                        boolean verified = com.acadscatchup.util.OtpVerifyDialog.show(
                                win, user.getEmail(), user.getFullName(), "Login 2FA Verification");
                        if (!verified) {
                            errorLabel.setText("Login cancelled or 2FA verification was not completed.");
                            return;
                        }
                    }

                    Session.setCurrentUser(user);
                    navigateToDashboard(user);
                }
        );
    }

    private void navigateToDashboard(User user) {
        try {
            // ADMIN and PROFESSOR both go to professor dashboard (full access)
            boolean isProfOrAdmin = "PROFESSOR".equals(user.getRole()) || "ADMIN".equals(user.getRole());
            String fxml = isProfOrAdmin
                    ? "/com/acadscatchup/fxml/prof_dashboard.fxml"
                    : "/com/acadscatchup/fxml/student_dashboard.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMinWidth(720);
            stage.setMinHeight(520);

            String roleLabel = switch (user.getRole()) {
                case "ADMIN"     -> "Admin";
                case "PROFESSOR" -> "Professor";
                default          -> "Student";
            };
            stage.setTitle("AcadsCatchUp — " + roleLabel + " Dashboard");
            com.acadscatchup.util.WindowUtil.initFullScreenWithCentering(stage, 1200, 720);

            com.acadscatchup.util.AppTrayManager.setCurrentStage(stage);
            stage.setOnCloseRequest(e -> {
                e.consume();
                com.acadscatchup.util.AppTrayManager.handleCloseRequest(stage);
            });

        } catch (IOException e) {
            errorLabel.setText("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }



    /** Opens the FAQ and Project Credits modal dialog. */
    @FXML
    private void handleShowFAQ() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/acadscatchup/fxml/faq_dialog.fxml"));
            Parent root = loader.load();
            com.acadscatchup.util.ModalOverlay.showAndWait(rootPane, root, 560, 620);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Allow pressing Enter on password field to trigger login. */
    @FXML
    private void initialize() {
        if (bgImageView != null && rootPane != null) {
            bgImageView.fitWidthProperty().bind(rootPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootPane.heightProperty());
        }
        com.acadscatchup.util.PasswordToggleHelper.setupExisting(
                passwordField,
                plainPasswordField,
                btnTogglePassword
        );
        passwordField.setOnAction(event -> handleLogin());
        if (plainPasswordField != null) {
            plainPasswordField.setOnAction(event -> handleLogin());
        }
        usernameField.setOnAction(event -> handleLogin());

        // Cross-platform emoji patching for Linux
        javafx.application.Platform.runLater(() -> com.acadscatchup.util.OSCompat.patchEmojis(rootPane));

        // Check internet connection on startup
        javafx.application.Platform.runLater(() -> {
            if (!com.acadscatchup.db.DBConnection.hasInternet()) {
                javafx.stage.Window win = (rootPane.getScene() != null) ? rootPane.getScene().getWindow() : null;
                com.acadscatchup.util.CustomAlert.showError(
                        win,
                        "No Internet Connection",
                        "⚠️ No Internet Connection Detected\n\nAcadsCatchUp requires an active internet connection to communicate with the online database.\n\nPlease connect your device to the internet before signing in."
                );
            }
        });
    }

    /**
     * Handles forgot password requests via Gmail OTP verification.
     */
    @FXML
    private void handleForgotPassword() {
        Window owner = (rootPane.getScene() != null) ? rootPane.getScene().getWindow() : null;

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1d2e; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #151825; -fx-padding: 16 20; -fx-background-radius: 12 12 0 0; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");
        Label iconLbl = new Label("🔑");
        iconLbl.setStyle("-fx-font-size: 22px;");
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Password Recovery");
        titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label subTitleLbl = new Label("Verify your identity via registered Gmail");
        subTitleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(titleLbl, subTitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconLbl, titleBox, spacer);

        // Content
        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 24, 16, 24));
        Label prompt = new Label("Enter your Username or Registered Gmail Address:");
        prompt.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        TextField tfIdentifier = new TextField();
        tfIdentifier.setPromptText("Username or Gmail");
        tfIdentifier.setStyle("-fx-font-size: 13px; -fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 8 12;");

        Label msgLbl = new Label();
        msgLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        msgLbl.setVisible(false);
        msgLbl.setManaged(false);

        content.getChildren().addAll(prompt, tfIdentifier, msgLbl);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 14 20; -fx-background-radius: 0 0 12 12; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> com.acadscatchup.util.ModalOverlay.close(cancelBtn));

        Button nextBtn = new Button("Send Reset Code ➔");
        nextBtn.getStyleClass().add("btn-primary");

        Runnable processLookup = () -> {
            String idVal = tfIdentifier.getText().trim();
            if (idVal.isEmpty()) {
                msgLbl.setText("Please enter your username or registered email.");
                msgLbl.setVisible(true); msgLbl.setManaged(true);
                return;
            }

            com.acadscatchup.util.GmailLookupUtil.ValidationResult lookup =
                    com.acadscatchup.util.GmailLookupUtil.lookupAccount(idVal);

            if (!lookup.isValid()) {
                msgLbl.setText(lookup.getMessage());
                msgLbl.setVisible(true); msgLbl.setManaged(true);
                return;
            }

            User target = lookup.getUser();
            com.acadscatchup.util.ModalOverlay.close(nextBtn);

            // Open OTP Verification Dialog
            boolean verified = com.acadscatchup.util.OtpVerifyDialog.show(
                    owner, target.getEmail(), target.getFullName(), "Password Reset");
            if (verified) {
                showSetNewPasswordDialog(owner, target);
            }
        };

        nextBtn.setOnAction(e -> processLookup.run());
        tfIdentifier.setOnAction(e -> processLookup.run());
        footer.getChildren().addAll(cancelBtn, nextBtn);

        root.getChildren().addAll(header, content, footer);
        com.acadscatchup.util.ModalOverlay.showAndWait(rootPane, root, 440, 320);
    }

    private void showSetNewPasswordDialog(Window owner, User target) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1d2e; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #151825; -fx-padding: 16 20; -fx-background-radius: 12 12 0 0; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");
        Label iconLbl = new Label("🔐");
        iconLbl.setStyle("-fx-font-size: 22px;");
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Set New Password");
        titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label subTitleLbl = new Label("Account: " + target.getUsername() + " (" + target.getFullName() + ")");
        subTitleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(titleLbl, subTitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(iconLbl, titleBox, spacer);

        // Content
        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 16, 24));

        Label p1Lbl = new Label("New Password:");
        p1Lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        com.acadscatchup.util.PasswordToggleHelper.PasswordBox pfBox1 =
                com.acadscatchup.util.PasswordToggleHelper.createPasswordBox(
                        "Enter new password",
                        "-fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 8 38 8 12;"
                );

        Label p2Lbl = new Label("Confirm Password:");
        p2Lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        com.acadscatchup.util.PasswordToggleHelper.PasswordBox pfBox2 =
                com.acadscatchup.util.PasswordToggleHelper.createPasswordBox(
                        "Re-type new password",
                        "-fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 8 38 8 12;"
                );

        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        errLbl.setVisible(false); errLbl.setManaged(false);

        content.getChildren().addAll(p1Lbl, pfBox1, p2Lbl, pfBox2, errLbl);

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 14 20; -fx-background-radius: 0 0 12 12; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setOnAction(e -> com.acadscatchup.util.ModalOverlay.close(cancelBtn));

        Button saveBtn = new Button("💾 Update Password");
        saveBtn.getStyleClass().add("btn-primary");

        saveBtn.setOnAction(e -> {
            String p1 = pfBox1.getText();
            String p2 = pfBox2.getText();
            if (p1.isEmpty() || p2.isEmpty()) {
                errLbl.setText("Please fill out both password fields.");
                errLbl.setVisible(true); errLbl.setManaged(true);
                return;
            }
            if (!p1.equals(p2)) {
                errLbl.setText("Passwords do not match.");
                errLbl.setVisible(true); errLbl.setManaged(true);
                return;
            }
            if (p1.length() < 4) {
                errLbl.setText("Password must be at least 4 characters.");
                errLbl.setVisible(true); errLbl.setManaged(true);
                return;
            }

            boolean ok = userDAO.updatePasswordByEmail(target.getEmail(), p1);
            if (ok) {
                com.acadscatchup.util.ModalOverlay.close(saveBtn);
                com.acadscatchup.util.CustomAlert.showInfo(owner,
                        "Password Updated",
                        "Your password has been successfully updated! You can now sign in with your new password.");
            } else {
                errLbl.setText("Failed to update password. Please try again.");
                errLbl.setVisible(true); errLbl.setManaged(true);
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(header, content, footer);

        com.acadscatchup.util.ModalOverlay.showAndWait(rootPane, root, 420, 360);
    }
}
