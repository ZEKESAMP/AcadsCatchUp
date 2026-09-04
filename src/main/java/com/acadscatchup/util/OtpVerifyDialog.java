package com.acadscatchup.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Reusable modal dialog for entering and verifying 6-digit Gmail OTP codes.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class OtpVerifyDialog {

    public static final String DEVELOPER = "F4TAL";

    private boolean isVerified = false;
    private int countdownSeconds = 60;
    private Timeline countdownTimeline;

    /**
     * Shows the OTP verification modal for the specified recipient.
     * Automatically triggers the initial OTP send.
     *
     * @return true if the user entered the correct OTP; false if cancelled.
     */
    public static boolean show(Window owner, String recipientEmail, String recipientName, String purpose) {
        OtpVerifyDialog dialog = new OtpVerifyDialog();
        return dialog.display(owner, recipientEmail, recipientName, purpose);
    }

    private boolean display(Window owner, String recipientEmail, String recipientName, String purpose) {
        // Initial OTP dispatch
        EmailService.OtpSendResult sendResult = EmailService.generateAndSendOtp(recipientEmail, recipientName, purpose);

        // If email delivery was rejected by the mail server, do NOT open the dialog!
        if (!sendResult.success) {
            CustomAlert.showError(owner, "Email Verification Failed",
                    sendResult.message != null ? sendResult.message : "Could not deliver verification code to " + recipientEmail);
            return false;
        }

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1a1d2e; -fx-background-radius: 12; -fx-border-color: #2d3255; -fx-border-width: 1.5; -fx-border-radius: 12;");

        // ── Header ──────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #151825; -fx-padding: 16 20; -fx-background-radius: 12 12 0 0; -fx-border-color: #2d3255; -fx-border-width: 0 0 1 0;");

        String p = (purpose != null) ? purpose.toLowerCase() : "";
        String iconText;
        String titleText;
        String subTitleText;

        if (p.contains("login") || p.contains("2fa")) {
            iconText = "🛡️";
            titleText = "Login 2FA Verification";
            subTitleText = "Authorize sign-in attempt via 6-digit code";
        } else if (p.contains("reset") || p.contains("password")) {
            iconText = "🔑";
            titleText = "Password Reset Verification";
            subTitleText = "Verify your identity to set a new password";
        } else if (p.contains("change") || p.contains("update") || p.contains("link") || p.contains("registration")) {
            iconText = "✉";
            titleText = "Email Change Authorization";
            subTitleText = "Confirm update to your primary Gmail";
        } else {
            iconText = "🎓";
            titleText = "First-Time Account Verification";
            subTitleText = "Enter 6-digit code sent via Gmail to activate";
        }

        Label iconLbl = new Label(iconText);
        iconLbl.setStyle("-fx-font-size: 22px;");

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label(titleText);
        titleLbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label subTitleLbl = new Label(subTitleText);
        subTitleLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(titleLbl, subTitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(iconLbl, titleBox, spacer);

        // ── Content ─────────────────────────────────────────────────────────
        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 24, 16, 24));
        content.setAlignment(Pos.CENTER);

        Label emailNotice = new Label("A 6-digit code for " + titleText + " was sent to:\n" + recipientEmail);
        emailNotice.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-text-alignment: center; -fx-alignment: center;");
        emailNotice.setWrapText(true);

        // Simulation banner if active
        VBox simBanner = new VBox(6);
        simBanner.setAlignment(Pos.CENTER);
        simBanner.setStyle("-fx-background-color: rgba(56, 189, 248, 0.1); -fx-padding: 8 12; -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 8;");
        Label simLbl = new Label("💡 Simulation Code: " + (sendResult.otpCode != null ? sendResult.otpCode : ""));
        simLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");
        simBanner.getChildren().add(simLbl);
        simBanner.setVisible(sendResult.isSimulation);
        simBanner.setManaged(sendResult.isSimulation);

        // OTP Code Input
        TextField tfOtp = new TextField();
        tfOtp.setPromptText("• • • • • •");
        tfOtp.setMaxWidth(220);
        tfOtp.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-alignment: CENTER; -fx-letter-spacing: 6px; -fx-font-family: 'Consolas', monospace; -fx-background-color: #121520; -fx-text-fill: #38bdf8; -fx-border-color: #2d3255; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");

        // Force numeric and limit to 6 digits
        tfOtp.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (filtered.length() > 6) {
                filtered = filtered.substring(0, 6);
            }
            if (!filtered.equals(newVal)) {
                tfOtp.setText(filtered);
            }
        });

        // Quick auto-fill button for simulation mode
        if (sendResult.isSimulation && sendResult.otpCode != null) {
            Button btnAutoFill = new Button("Auto-Fill Simulation Code");
            btnAutoFill.setStyle("-fx-background-color: transparent; -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-underline: true; -fx-cursor: hand;");
            btnAutoFill.setOnAction(e -> tfOtp.setText(sendResult.otpCode));
            simBanner.getChildren().add(btnAutoFill);
        }

        // Error message label
        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        // Resend countdown button
        Button btnResend = new Button("Resend Code (60s)");
        btnResend.setDisable(true);
        btnResend.getStyleClass().add("btn-ghost");
        btnResend.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        countdownSeconds = 60;
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            countdownSeconds--;
            if (countdownSeconds > 0) {
                btnResend.setText("Resend Code (" + countdownSeconds + "s)");
            } else {
                btnResend.setText("🔄 Resend Code");
                btnResend.setDisable(false);
                countdownTimeline.stop();
            }
        }));
        countdownTimeline.setCycleCount(60);
        countdownTimeline.play();

        btnResend.setOnAction(e -> {
            btnResend.setDisable(true);
            countdownSeconds = 60;
            countdownTimeline.playFromStart();
            EmailService.OtpSendResult resendResult = EmailService.generateAndSendOtp(recipientEmail, recipientName, purpose);
            if (!resendResult.success) {
                errorLbl.setText(resendResult.message != null ? resendResult.message : "Failed to deliver email to this address.");
                errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }
            if (resendResult.isSimulation && resendResult.otpCode != null) {
                simLbl.setText("💡 Simulation Code: " + resendResult.otpCode);
                simBanner.setVisible(true);
                simBanner.setManaged(true);
            }
            errorLbl.setText("New verification code dispatched!");
            errorLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px;");
            errorLbl.setVisible(true);
            errorLbl.setManaged(true);
        });

        content.getChildren().addAll(emailNotice, simBanner, tfOtp, errorLbl, btnResend);

        // ── Footer ──────────────────────────────────────────────────────────
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #151825; -fx-padding: 14 20; -fx-background-radius: 0 0 12 12; -fx-border-color: #2d3255; -fx-border-width: 1 0 0 0;");

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn-ghost");
        btnCancel.setOnAction(e -> {
            if (countdownTimeline != null) countdownTimeline.stop();
            ModalOverlay.close(btnCancel);
        });

        Button btnVerify = new Button("✔ Verify & Proceed");
        btnVerify.getStyleClass().add("btn-primary");
        btnVerify.setStyle("-fx-font-weight: bold; -fx-padding: 8 18;");

        Runnable doVerify = () -> {
            String entered = tfOtp.getText().trim();
            if (entered.length() != 6) {
                errorLbl.setText("Please enter the complete 6-digit code.");
                errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            EmailService.VerificationStatus status = EmailService.verifyOtp(recipientEmail, entered);
            switch (status) {
                case SUCCESS -> {
                    isVerified = true;
                    if (countdownTimeline != null) countdownTimeline.stop();
                    ModalOverlay.close(btnVerify);
                }
                case INVALID_CODE -> {
                    errorLbl.setText("Invalid verification code. Please try again.");
                    errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                    tfOtp.selectAll();
                }
                case EXPIRED -> {
                    errorLbl.setText("Verification code expired. Please click Resend Code.");
                    errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                }
                case TOO_MANY_ATTEMPTS -> {
                    errorLbl.setText("Too many incorrect attempts. Please request a new code.");
                    errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                }
                case NOT_FOUND -> {
                    errorLbl.setText("No active verification session found. Please resend.");
                    errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
                    errorLbl.setVisible(true);
                    errorLbl.setManaged(true);
                }
            }
        };

        btnVerify.setOnAction(e -> doVerify.run());
        tfOtp.setOnAction(e -> doVerify.run());

        footer.getChildren().addAll(btnCancel, btnVerify);

        root.getChildren().addAll(header, content, footer);

        Node anchor = (owner != null && owner.getScene() != null) ? owner.getScene().getRoot() : null;
        ModalOverlay.showAndWait(anchor, root, 440, 420);
        if (countdownTimeline != null) countdownTimeline.stop();
        return isVerified;
    }
}
