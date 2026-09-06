package com.acadscatchup.util;

import com.acadscatchup.dao.UserDAO;
import com.acadscatchup.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

/**
 * Account & Security Settings modal dialog for both Students and Professors.
 * Allows adding/updating registered Gmail with OTP verification,
 * and changing account password with mandatory OTP security verification.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AccountSettingsDialog {

    public static final String DEVELOPER = "F4TAL";

    public static void show(Window owner) {
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) return;

        UserDAO userDAO = new UserDAO();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1d2e; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        // ── Header ──────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #151825; -fx-padding: 16 22; -fx-background-radius: 12 12 0 0; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");

        Label iconLbl = new Label("⚙");
        iconLbl.setStyle("-fx-font-size: 24px;");

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Account & Security Settings");
        titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label subTitleLbl = new Label("Manage your registered Gmail and update credentials");
        subTitleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(titleLbl, subTitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(iconLbl, titleBox, spacer);

        // ── Scrollable Body ─────────────────────────────────────────────────
        VBox content = new VBox(18);
        content.setPadding(new Insets(20, 24, 20, 24));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1d2e; -fx-border-color: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ── User Profile Badge ──────────────────────────────────────────────
        HBox profileCard = new HBox(12);
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setStyle("-fx-background-color: #121520; -fx-padding: 12 16; -fx-border-color: #2d3255; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label avatar = new Label(currentUser.isProfessor() ? "👨‍🏫" : (currentUser.isAdmin() ? "🛡️" : "🎓"));
        avatar.setStyle("-fx-font-size: 24px;");

        VBox profileInfo = new VBox(3);
        Label nameLbl = new Label(currentUser.getFullName() + " (@" + currentUser.getUsername() + ")");
        nameLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14px; -fx-font-weight: bold;");

        String roleDetails = currentUser.getRole();
        if (currentUser.isStudent() && currentUser.getProgram() != null) {
            roleDetails += " • " + currentUser.getProgram() + (currentUser.getYearLevel() > 0 ? " • " + currentUser.getYearDisplay() : "");
        }
        Label roleLbl = new Label(roleDetails);
        roleLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");
        profileInfo.getChildren().addAll(nameLbl, roleLbl);

        profileCard.getChildren().addAll(avatar, profileInfo);

        // ── SECTION 1: Registered Gmail Address ─────────────────────────────
        VBox emailSection = new VBox(10);
        emailSection.setStyle("-fx-background-color: rgba(30, 41, 59, 0.4); -fx-padding: 14 16; -fx-border-color: #2d3255; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label emailSecTitle = new Label("📧 Registered Gmail Address");
        emailSecTitle.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label currentEmailStatus = new Label();
        Runnable refreshEmailStatus = () -> {
            String currEmail = currentUser.getEmail();
            if (currEmail != null && !currEmail.isBlank()) {
                currentEmailStatus.setText("✔ Verified: " + currEmail);
                currentEmailStatus.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: bold;");
            } else {
                currentEmailStatus.setText("⚠️ No Gmail registered yet. Link your Gmail to enable password recovery & OTP security.");
                currentEmailStatus.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px;");
            }
        };
        refreshEmailStatus.run();

        HBox emailInputRow = new HBox(10);
        emailInputRow.setAlignment(Pos.CENTER_LEFT);

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("Enter your @gmail.com address");
        tfEmail.setStyle("-fx-font-size: 12.5px; -fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 7 12;");
        HBox.setHgrow(tfEmail, Priority.ALWAYS);

        Button btnVerifyEmail = new Button("📩 Verify & Save");
        btnVerifyEmail.getStyleClass().add("btn-primary");
        btnVerifyEmail.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-padding: 7 14;");

        emailInputRow.getChildren().addAll(tfEmail, btnVerifyEmail);

        Label emailMsg = new Label();
        emailMsg.setStyle("-fx-font-size: 11.5px;");
        emailMsg.setVisible(false); emailMsg.setManaged(false);

        btnVerifyEmail.setOnAction(e -> {
            String newEmail = tfEmail.getText().trim();
            GmailLookupUtil.ValidationResult valRes = GmailLookupUtil.validateEmail(newEmail);
            if (!valRes.isValid()) {
                emailMsg.setText(valRes.getMessage());
                emailMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                emailMsg.setVisible(true); emailMsg.setManaged(true);
                return;
            }
            if (newEmail.equalsIgnoreCase(currentUser.getEmail())) {
                emailMsg.setText("This email is already registered to your account.");
                emailMsg.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11.5px;");
                emailMsg.setVisible(true); emailMsg.setManaged(true);
                return;
            }
            if (userDAO.isEmailTaken(newEmail, currentUser.getId())) {
                emailMsg.setText("This email is already registered to another user.");
                emailMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                emailMsg.setVisible(true); emailMsg.setManaged(true);
                return;
            }

            emailMsg.setVisible(false); emailMsg.setManaged(false);

            // Trigger OTP verification
            boolean verified = OtpVerifyDialog.show(owner, newEmail, currentUser.getFullName(), "Email Change Authorization");
            if (verified) {
                boolean ok = userDAO.updateUserEmail(currentUser.getId(), newEmail);
                if (ok) {
                    currentUser.setEmail(newEmail);
                    refreshEmailStatus.run();
                    tfEmail.clear();
                    emailMsg.setText("✔ Gmail verified and successfully linked to your account!");
                    emailMsg.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11.5px; -fx-font-weight: bold;");
                    emailMsg.setVisible(true); emailMsg.setManaged(true);
                    CustomAlert.showInfo(owner, "Gmail Verified & Saved",
                            "Your Gmail address (" + newEmail + ") has been verified and registered successfully!");
                } else {
                    emailMsg.setText("Failed to save email to database.");
                    emailMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                    emailMsg.setVisible(true); emailMsg.setManaged(true);
                }
            } else {
                emailMsg.setText("Verification was not completed. Email was not changed.");
                emailMsg.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11.5px;");
                emailMsg.setVisible(true); emailMsg.setManaged(true);
            }
        });

        emailSection.getChildren().addAll(emailSecTitle, currentEmailStatus, emailInputRow, emailMsg);

        // ── SECTION 2: Change Password with OTP ─────────────────────────────
        VBox passSection = new VBox(10);
        passSection.setStyle("-fx-background-color: rgba(30, 41, 59, 0.4); -fx-padding: 14 16; -fx-border-color: #2d3255; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label passSecTitle = new Label("🔑 Change Password (OTP Protected)");
        passSecTitle.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label passHint = new Label("A 6-digit OTP will be sent to your verified Gmail to authorize the password change.");
        passHint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        GridPane passGrid = new GridPane();
        passGrid.setHgap(10); passGrid.setVgap(8);

        Label lblCurr = new Label("Current Password:");
        lblCurr.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        PasswordToggleHelper.PasswordBox pfCurr = PasswordToggleHelper.createPasswordBox(
                "Your existing password",
                "-fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 6 36 6 10;"
        );

        Label lblNew = new Label("New Password:");
        lblNew.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        PasswordToggleHelper.PasswordBox pfNew = PasswordToggleHelper.createPasswordBox(
                "Enter your New Password",
                "-fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 6 36 6 10;"
        );

        Label lblConf = new Label("Confirm Password:");
        lblConf.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        PasswordToggleHelper.PasswordBox pfConf = PasswordToggleHelper.createPasswordBox(
                "Re-type new password",
                "-fx-background-color: #121520; -fx-text-fill: #f8fafc; -fx-border-color: #2d3255; -fx-border-radius: 6; -fx-padding: 6 36 6 10;"
        );

        passGrid.addRow(0, lblCurr, pfCurr);
        passGrid.addRow(1, lblNew,  pfNew);
        passGrid.addRow(2, lblConf, pfConf);

        Label passMsg = new Label();
        passMsg.setStyle("-fx-font-size: 11.5px;");
        passMsg.setVisible(false); passMsg.setManaged(false);

        HBox passBtnBox = new HBox();
        passBtnBox.setAlignment(Pos.CENTER_RIGHT);
        Button btnChangePass = new Button("🔒 Verify OTP & Update Password");
        btnChangePass.getStyleClass().add("btn-primary");
        btnChangePass.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-padding: 7 14;");
        passBtnBox.getChildren().add(btnChangePass);

        btnChangePass.setOnAction(e -> {
            String currP = pfCurr.getText();
            String newP  = pfNew.getText();
            String confP = pfConf.getText();

            if (currP.isEmpty() || newP.isEmpty() || confP.isEmpty()) {
                passMsg.setText("Please fill out all three password fields.");
                passMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                passMsg.setVisible(true); passMsg.setManaged(true);
                return;
            }
            if (!newP.equals(confP)) {
                passMsg.setText("New passwords do not match.");
                passMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                passMsg.setVisible(true); passMsg.setManaged(true);
                return;
            }
            if (newP.length() < 4) {
                passMsg.setText("New password must be at least 4 characters.");
                passMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                passMsg.setVisible(true); passMsg.setManaged(true);
                return;
            }

            // Verify current password against database
            if (!userDAO.verifyUserPassword(currentUser.getId(), currP)) {
                passMsg.setText("Current password is incorrect.");
                passMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                passMsg.setVisible(true); passMsg.setManaged(true);
                return;
            }

            // Must have a registered Gmail for OTP verification
            String userEmail = currentUser.getEmail();
            if (userEmail == null || userEmail.isBlank()) {
                passMsg.setText("Please add and verify your Gmail address above before changing your password.");
                passMsg.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11.5px;");
                passMsg.setVisible(true); passMsg.setManaged(true);
                return;
            }

            passMsg.setVisible(false); passMsg.setManaged(false);

            // Require OTP verification before changing password!
            boolean verified = OtpVerifyDialog.show(owner, userEmail, currentUser.getFullName(), "Password Change Authorization");
            if (verified) {
                boolean ok = userDAO.updatePassword(currentUser.getId(), newP);
                if (ok) {
                    pfCurr.clear(); pfNew.clear(); pfConf.clear();
                    passMsg.setText("✔ Password updated successfully!");
                    passMsg.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11.5px; -fx-font-weight: bold;");
                    passMsg.setVisible(true); passMsg.setManaged(true);
                    CustomAlert.showInfo(owner, "Password Changed", "Your password has been successfully updated!");
                } else {
                    passMsg.setText("Failed to update password in database.");
                    passMsg.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11.5px;");
                    passMsg.setVisible(true); passMsg.setManaged(true);
                }
            }
        });

        passSection.getChildren().addAll(passSecTitle, passHint, passGrid, passMsg, passBtnBox);

        content.getChildren().addAll(profileCard, emailSection, passSection);

        // ── Footer ──────────────────────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 12 22; -fx-background-radius: 0 0 12 12; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button btnCheckUpdate = new Button("🔄 Check for Updates (v" + UpdateSplash.CURRENT_VERSION + ")");
        btnCheckUpdate.getStyleClass().add("btn-ghost");
        btnCheckUpdate.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #949ba4;");
        btnCheckUpdate.setOnAction(e -> UpdateSplash.checkManual(owner));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button doneBtn = new Button("Close");
        doneBtn.getStyleClass().add("btn-primary");
        doneBtn.setOnAction(e -> ModalOverlay.close(doneBtn));
        footer.getChildren().addAll(btnCheckUpdate, footerSpacer, doneBtn);

        root.getChildren().addAll(header, scrollPane, footer);

        Node anchor = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : null;
        ModalOverlay.showAndWait(anchor, root, 520, 640);
    }
}
