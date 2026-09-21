package com.acadscatchup.util;

/**
 * Unit test for EmailService OTP resolution, validation, and security workflows.
 * SQA Verification for Capstone Evaluation.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class EmailServiceTest {

    public static final String DEVELOPER = "F4TAL";

    public void testResolvePurposeLogin() {
        EmailService.OtpPurposeDetails details = EmailService.resolvePurposeDetails("login", "Stevenson");
        assert details != null : "Details must not be null";
        assert details.subject().contains("2-Step Verification") : "Subject must reflect 2FA login";
        assert "LOGIN 2FA SECURITY".equals(details.badgeText()) : "Badge text must match";
        assert details.descriptionText().contains("Dear Stevenson,") : "Salutation must include name";
    }

    public void testResolvePurposeReset() {
        EmailService.OtpPurposeDetails details = EmailService.resolvePurposeDetails("password_reset", "Alex");
        assert details != null : "Details must not be null";
        assert details.subject().contains("Password Reset") : "Subject must reflect password reset";
        assert "PASSWORD RESET".equals(details.badgeText()) : "Badge text must match";
    }

    public void testResolvePurposeEmailChange() {
        EmailService.OtpPurposeDetails details = EmailService.resolvePurposeDetails("change_email", "Student");
        assert details != null : "Details must not be null";
        assert details.subject().contains("Email Change Authorization") : "Subject must reflect email change";
        assert "EMAIL ADDRESS UPDATE".equals(details.badgeText()) : "Badge text must match";
    }

    public void testResolvePurposeDefaultActivation() {
        EmailService.OtpPurposeDetails details = EmailService.resolvePurposeDetails(null, "");
        assert details != null : "Details must not be null";
        assert details.subject().contains("Account Verification Code") : "Default subject must reflect account activation";
        assert details.descriptionText().contains("Hello,") : "Empty name should fall back to Hello,";
    }

    public void testVerifyOtpWithNullOrEmpty() {
        EmailService.VerificationStatus status1 = EmailService.verifyOtp(null, "123456");
        assert status1 == EmailService.VerificationStatus.NOT_FOUND : "Null email must return NOT_FOUND";

        EmailService.VerificationStatus status2 = EmailService.verifyOtp("test@example.com", null);
        assert status2 == EmailService.VerificationStatus.NOT_FOUND : "Null code must return NOT_FOUND";
    }
}
