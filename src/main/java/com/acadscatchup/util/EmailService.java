package com.acadscatchup.util;

import com.acadscatchup.db.DBConnection;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure Java Gmail SMTP client and OTP security engine.
 * Communicates directly with smtp.gmail.com:465 over SSL without external dependencies.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class EmailService {

    public static final String DEVELOPER = "F4TAL";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 465; // SSL port

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, OtpEntry> OTP_CACHE = new ConcurrentHashMap<>();

    // Obfuscated cloud email relay configuration (Protected against decompiler inspection)
    private static final int OBF_KEY_VAL = 0x5A;
    private static final int[] OBF_LOGIN  = new int[]{56,109,56,60,104,63,106,106,107,26,41,55,46,42,119,56,40,63,44,53,116,57,53,55};
    private static final int[] OBF_KEY    = new int[]{34,41,55,46,42,41,51,56,119,99,105,60,108,111,56,98,98,107,111,57,63,60,62,105,63,107,106,59,98,111,60,63,106,107,110,106,107,106,111,104,59,108,110,105,62,107,56,62,107,111,110,106,106,60,56,59,105,63,60,63,98,110,111,98,108,105,99,63,60,110,56,98,108,119,30,106,99,53,24,63,31,61,11,25,61,35,62,55,25,50};
    private static final int[] OBF_SENDER = new int[]{40,59,44,63,52,42,54,59,35,32,106,26,61,55,59,51,54,116,57,53,55};

    private static String decode(int[] data, int key) {
        char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) (data[i] ^ key);
        }
        return new String(chars);
    }

    private static final String DEFAULT_BREVO_LOGIN = decode(OBF_LOGIN, OBF_KEY_VAL);
    private static final String DEFAULT_BREVO_KEY   = decode(OBF_KEY, OBF_KEY_VAL);
    private static final String DEFAULT_SENDER      = decode(OBF_SENDER, OBF_KEY_VAL);

    private static String cachedSenderEmail = DEFAULT_SENDER;
    private static String cachedAppPassword = DEFAULT_BREVO_KEY;
    private static String cachedApiKey = DEFAULT_BREVO_LOGIN;
    private static boolean cachedIs2FAEnabled = false;
    private static boolean configLoaded = false;

    public record OtpEntry(String code, long expiryMillis, int attempts) {}

    public record OtpPurposeDetails(String subject, String badgeText, String headerTitle, String descriptionText, String securityNotice) {}

    public static OtpPurposeDetails resolvePurposeDetails(String purpose, String recipientName) {
        String p = (purpose != null) ? purpose.toLowerCase().trim() : "";
        String salutation = (recipientName != null && !recipientName.isBlank()) ? "Dear " + recipientName + "," : "Hello,";

        if (p.contains("login") || p.contains("2fa")) {
            return new OtpPurposeDetails(
                    "Acads Catch Up — 2-Step Verification Login Code",
                    "LOGIN 2FA SECURITY",
                    "Sign-In Verification",
                    salutation + "<br><br>A sign-in attempt was initiated for your Acads Catch Up account. To complete your login and protect your account, please enter the 6-digit One-Time Password (OTP) below.",
                    "If you did not initiate this sign-in attempt, someone may have access to your credentials. Please change your password immediately after securing your account."
            );
        } else if (p.contains("reset") || p.contains("password")) {
            return new OtpPurposeDetails(
                    "Acads Catch Up — Password Reset Verification Code",
                    "PASSWORD RESET",
                    "Reset Your Password",
                    salutation + "<br><br>We received a request to reset the password for your Acads Catch Up account. Use the 6-digit One-Time Password (OTP) below to authorize this password reset.",
                    "If you did not submit a password reset request, you can safely disregard this email. Your account credentials remain safe and unchanged."
            );
        } else if (p.contains("change") || p.contains("update") || p.contains("registration") || p.contains("link")) {
            return new OtpPurposeDetails(
                    "Acads Catch Up — Email Change Authorization Code",
                    "EMAIL ADDRESS UPDATE",
                    "Verify Email Address",
                    salutation + "<br><br>A request was made to register or update the primary Gmail address linked to your Acads Catch Up account. Please enter the 6-digit One-Time Password (OTP) below to authorize this change.",
                    "If you did not authorize this email update, please contact your course professor or system administrator immediately."
            );
        } else {
            // Default: First-Time Account Verification
            return new OtpPurposeDetails(
                    "Acads Catch Up — Account Verification Code",
                    "FIRST-TIME ACCOUNT VERIFICATION",
                    "Activate Your Account",
                    salutation + "<br><br>Welcome to Acads Catch Up! To activate your student account and access your enrolled subjects and academic deadlines, please complete verification with the 6-digit One-Time Password (OTP) below.",
                    "If you were not expecting this account activation email, please ignore this message."
            );
        }
    }

    public enum VerificationStatus {
        SUCCESS,
        INVALID_CODE,
        EXPIRED,
        NOT_FOUND,
        TOO_MANY_ATTEMPTS
    }

    public static class OtpSendResult {
        public final boolean success;
        public final boolean isSimulation;
        public final String otpCode;
        public final String message;

        public OtpSendResult(boolean success, boolean isSimulation, String otpCode, String message) {
            this.success = success;
            this.isSimulation = isSimulation;
            this.otpCode = otpCode;
            this.message = message;
        }
    }

    // ── Configuration Management ──────────────────────────────────────────

    public static synchronized void ensureConfigLoaded() {
        if (configLoaded) return;
        loadConfig();
    }

    public static synchronized void loadConfig() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT sender_email, app_password, api_key, is_2fa_enabled FROM email_config WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String sEmail = rs.getString("sender_email");
                String sPass  = rs.getString("app_password");
                String sKey   = rs.getString("api_key");
                cachedSenderEmail   = (sEmail != null && !sEmail.isBlank()) ? sEmail.trim() : DEFAULT_SENDER;
                cachedAppPassword   = (sPass != null && !sPass.isBlank())   ? sPass.trim()  : DEFAULT_BREVO_KEY;
                cachedApiKey        = (sKey != null && !sKey.isBlank())     ? sKey.trim()   : DEFAULT_BREVO_LOGIN;
                cachedIs2FAEnabled  = rs.getInt("is_2fa_enabled") == 1;
            } else {
                cachedSenderEmail   = DEFAULT_SENDER;
                cachedAppPassword   = DEFAULT_BREVO_KEY;
                cachedApiKey        = DEFAULT_BREVO_LOGIN;
            }
            configLoaded = true;
        } catch (Exception e) {
            System.err.println("[EmailService] loadConfig warning: " + e.getMessage());
            cachedSenderEmail   = DEFAULT_SENDER;
            cachedAppPassword   = DEFAULT_BREVO_KEY;
            cachedApiKey        = DEFAULT_BREVO_LOGIN;
            configLoaded = true;
        }
    }

    public static synchronized boolean saveConfig(String senderEmail, String appPassword, String apiKey, boolean is2FAEnabled) {
        String cleanEmail  = senderEmail != null ? senderEmail.trim() : "";
        String cleanPass   = appPassword != null ? appPassword.replaceAll("\\s+", "").trim() : "";
        String cleanApiKey = apiKey != null ? apiKey.trim() : "";
        String sql = DBConnection.isMySQL()
                ? "INSERT INTO email_config (id, sender_email, app_password, api_key, is_2fa_enabled) VALUES (1, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE sender_email=?, app_password=?, api_key=?, is_2fa_enabled=?"
                : "INSERT OR REPLACE INTO email_config (id, sender_email, app_password, api_key, is_2fa_enabled) VALUES (1, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanEmail);
            ps.setString(2, cleanPass);
            ps.setString(3, cleanApiKey);
            ps.setInt(4, is2FAEnabled ? 1 : 0);
            if (DBConnection.isMySQL()) {
                ps.setString(5, cleanEmail);
                ps.setString(6, cleanPass);
                ps.setString(7, cleanApiKey);
                ps.setInt(8, is2FAEnabled ? 1 : 0);
            }
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                cachedSenderEmail  = cleanEmail;
                cachedAppPassword  = cleanPass;
                cachedApiKey       = cleanApiKey;
                cachedIs2FAEnabled = is2FAEnabled;
                configLoaded = true;
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("[EmailService] saveConfig error: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean saveConfig(String senderEmail, String appPassword, boolean is2FAEnabled) {
        return saveConfig(senderEmail, appPassword, cachedApiKey, is2FAEnabled);
    }

    public static String getSenderEmail() {
        ensureConfigLoaded();
        return cachedSenderEmail;
    }

    public static String getAppPassword() {
        ensureConfigLoaded();
        return cachedAppPassword;
    }

    public static String getApiKey() {
        ensureConfigLoaded();
        return cachedApiKey;
    }

    public static boolean is2FAEnabled() {
        ensureConfigLoaded();
        return cachedIs2FAEnabled;
    }

    public static boolean isConfigured() {
        ensureConfigLoaded();
        boolean hasBrevo = cachedApiKey != null && !cachedApiKey.isBlank();
        boolean hasGmail = !cachedSenderEmail.isBlank() && !cachedAppPassword.isBlank();
        return hasBrevo || hasGmail;
    }

    // ── OTP Generation & Dispatch ─────────────────────────────────────────

    /**
     * Generates a 6-digit OTP, caches it with a 5-minute lifespan, and sends it via Brevo or Gmail.
     * If neither is configured, activates simulation mode (safe development fallback).
     */
    public static OtpSendResult generateAndSendOtp(String recipientEmail, String recipientName, String purpose) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return new OtpSendResult(false, false, null, "Recipient email is required.");
        }

        String emailKey = recipientEmail.trim().toLowerCase();
        String code = String.valueOf(RANDOM.nextInt(900000) + 100000); // 6-digit code
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes

        OTP_CACHE.put(emailKey, new OtpEntry(code, expiry, 0));

        ensureConfigLoaded();

        OtpPurposeDetails details = resolvePurposeDetails(purpose, recipientName);

        if (!isConfigured()) {
            System.out.println("[EmailService - SIMULATION MODE] OTP for " + recipientEmail + " is: [" + code + "] (Purpose: " + details.headerTitle() + ")");
            return new OtpSendResult(true, true, code,
                    "No email service configured yet. Running in Simulation Mode.\nYour verification code is: " + code);
        }

        try {
            String htmlContent = buildOtpHtml(code, details);

            if (cachedAppPassword != null && cachedAppPassword.startsWith("xsmtpsib-")) {
                String brevoLogin = (!cachedApiKey.isBlank()) ? cachedApiKey : "b7bf2e001@smtp-brevo.com";
                String sender = (!cachedSenderEmail.isBlank()) ? cachedSenderEmail : "ravenplayz0@gmail.com";
                sendBrevoSmtpEmail(
                        brevoLogin,
                        cachedAppPassword,
                        sender,
                        recipientEmail.trim(),
                        details.subject(),
                        htmlContent
                );
            } else if (cachedApiKey != null && cachedApiKey.startsWith("xkeysib-")) {
                String sender = (!cachedSenderEmail.isBlank()) ? cachedSenderEmail : "ravenplayz0@gmail.com";
                sendBrevoEmail(
                        cachedApiKey,
                        sender,
                        recipientEmail.trim(),
                        details.subject(),
                        htmlContent
                );
            } else {
                sendSmtpEmail(
                        cachedSenderEmail,
                        cachedAppPassword,
                        recipientEmail.trim(),
                        details.subject(),
                        htmlContent
                );
            }
            return new OtpSendResult(true, false, code, details.headerTitle() + " code sent to " + recipientEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Delivery failed: " + e.getMessage());
            // Fallback to simulation mode if delivery failed due to network / invalid credentials
            return new OtpSendResult(true, true, code,
                    "Could not send email (" + e.getMessage() + ").\nSimulation OTP: " + code);
        }
    }

    /**
     * Verifies the entered OTP against the cached code.
     */
    public static VerificationStatus verifyOtp(String recipientEmail, String enteredCode) {
        if (recipientEmail == null || enteredCode == null) return VerificationStatus.NOT_FOUND;
        String emailKey = recipientEmail.trim().toLowerCase();
        OtpEntry entry = OTP_CACHE.get(emailKey);

        if (entry == null) {
            return VerificationStatus.NOT_FOUND;
        }

        if (System.currentTimeMillis() > entry.expiryMillis()) {
            OTP_CACHE.remove(emailKey);
            return VerificationStatus.EXPIRED;
        }

        if (entry.attempts() >= 5) {
            OTP_CACHE.remove(emailKey);
            return VerificationStatus.TOO_MANY_ATTEMPTS;
        }

        if (entry.code().equals(enteredCode.trim())) {
            OTP_CACHE.remove(emailKey);
            return VerificationStatus.SUCCESS;
        } else {
            OTP_CACHE.put(emailKey, new OtpEntry(entry.code(), entry.expiryMillis(), entry.attempts() + 1));
            return VerificationStatus.INVALID_CODE;
        }
    }

    // ── Brevo SMTP & REST API Integration ───────────────────────────────────

    public static void sendBrevoSmtpEmail(String smtpLogin, String smtpKey, String senderEmail,
                                          String recipientEmail, String subject, String htmlBody) throws Exception {
        String host = "smtp-relay.brevo.com";
        int port = 587;

        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(12000);

            readUntilFinished(reader);

            sendCommand(writer, "EHLO localhost");
            readUntilFinished(reader);

            sendCommand(writer, "STARTTLS");
            String startTlsResp = reader.readLine();
            if (startTlsResp == null || !startTlsResp.startsWith("220")) {
                throw new IOException("STARTTLS failed: " + startTlsResp);
            }

            // Upgrade to TLS / SSL
            SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) ssf.createSocket(socket, host, port, true);
            sslSocket.setUseClientMode(true);
            sslSocket.startHandshake();

            BufferedReader sslReader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter sslWriter = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8));

            sendCommand(sslWriter, "EHLO localhost");
            readUntilFinished(sslReader);

            sendCommand(sslWriter, "AUTH LOGIN");
            expectReply(sslReader, 334);

            sendCommand(sslWriter, Base64.getEncoder().encodeToString(smtpLogin.getBytes(StandardCharsets.UTF_8)));
            expectReply(sslReader, 334);

            sendCommand(sslWriter, Base64.getEncoder().encodeToString(smtpKey.getBytes(StandardCharsets.UTF_8)));
            expectReply(sslReader, 235); // Authentication succeeded

            sendCommand(sslWriter, "MAIL FROM:<" + senderEmail + ">");
            expectReply(sslReader, 250);

            sendCommand(sslWriter, "RCPT TO:<" + recipientEmail + ">");
            expectReply(sslReader, 250);

            sendCommand(sslWriter, "DATA");
            expectReply(sslReader, 354);

            sslWriter.write("From: Acads Catch Up <" + senderEmail + ">\r\n");
            sslWriter.write("To: <" + recipientEmail + ">\r\n");
            sslWriter.write("Subject: " + subject + "\r\n");
            sslWriter.write("MIME-Version: 1.0\r\n");
            sslWriter.write("Content-Type: text/html; charset=UTF-8\r\n\r\n");
            sslWriter.write(htmlBody);
            sslWriter.write("\r\n.\r\n");
            sslWriter.flush();

            expectReply(sslReader, 250);

            sendCommand(sslWriter, "QUIT");
        }
    }

    public static void testBrevoConnection(String apiKey, String senderEmail, String testRecipient) throws Exception {
        OtpPurposeDetails details = resolvePurposeDetails("Connection Test", "Administrator");
        if (apiKey != null && apiKey.startsWith("xsmtpsib-")) {
            sendBrevoSmtpEmail(
                    "b7bf2e001@smtp-brevo.com",
                    apiKey,
                    senderEmail != null && !senderEmail.isBlank() ? senderEmail : "ravenplayz0@gmail.com",
                    testRecipient,
                    details.subject(),
                    buildOtpHtml("123456", details)
            );
        } else {
            sendBrevoEmail(
                    apiKey,
                    senderEmail != null && !senderEmail.isBlank() ? senderEmail : "notification@acadscatchup.edu",
                    testRecipient,
                    details.subject(),
                    buildOtpHtml("123456", details)
            );
        }
    }

    public static void sendBrevoEmail(String apiKey, String senderEmail,
                                      String recipientEmail, String subject, String htmlBody) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        String jsonPayload = "{"
                + "\"sender\":{\"name\":\"Acads Catch Up\",\"email\":\"" + escapeJson(senderEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(recipientEmail) + "\"}],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"htmlContent\":\"" + escapeJson(htmlBody) + "\""
                + "}";

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("accept", "application/json")
                .header("api-key", apiKey.trim())
                .header("content-type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofSeconds(12))
                .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Brevo API Error (" + response.statusCode() + "): " + response.body());
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    // ── Low-Level Pure Java SMTP Engine over SSL (Port 465) ─────────────────

    public static void testSmtpConnection(String sender, String appPassword, String testRecipient) throws Exception {
        String cleanSender = sender != null ? sender.trim() : "";
        String cleanPass   = appPassword != null ? appPassword.replaceAll("\\s+", "").trim() : "";
        OtpPurposeDetails details = resolvePurposeDetails("Connection Test", "Administrator");
        sendSmtpEmail(
                cleanSender,
                cleanPass,
                testRecipient != null ? testRecipient.trim() : cleanSender,
                details.subject(),
                buildOtpHtml("123456", details)
        );
    }

    public static void sendSmtpEmail(String senderEmail, String appPassword,
                                     String recipientEmail, String subject, String htmlBody) throws Exception {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(SMTP_HOST, SMTP_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(12000);

            // Read SMTP Server Greeting
            expectReply(reader, 220);

            // Handshake
            sendCommand(writer, "EHLO " + SMTP_HOST);
            readUntilFinished(reader);

            // Authenticate using AUTH LOGIN (Base64 credentials)
            sendCommand(writer, "AUTH LOGIN");
            expectReply(reader, 334);

            String encodedUser = Base64.getEncoder().encodeToString(senderEmail.getBytes(StandardCharsets.UTF_8));
            sendCommand(writer, encodedUser);
            expectReply(reader, 334);

            String encodedPass = Base64.getEncoder().encodeToString(appPassword.getBytes(StandardCharsets.UTF_8));
            sendCommand(writer, encodedPass);
            expectReply(reader, 235); // Authentication successful

            sendCommand(writer, "MAIL FROM:<" + senderEmail + ">");
            expectReply(reader, 250);

            sendCommand(writer, "RCPT TO:<" + recipientEmail + ">");
            expectReply(reader, 250);

            sendCommand(writer, "DATA");
            expectReply(reader, 354);

            // Transmit MIME Message Headers & Body
            writer.write("From: Acads Catch Up <" + senderEmail + ">\r\n");
            writer.write("To: <" + recipientEmail + ">\r\n");
            writer.write("Subject: " + subject + "\r\n");
            writer.write("MIME-Version: 1.0\r\n");
            writer.write("Content-Type: text/html; charset=UTF-8\r\n");
            writer.write("Content-Transfer-Encoding: 8bit\r\n");
            writer.write("\r\n");
            writer.write(htmlBody);
            writer.write("\r\n.\r\n");
            writer.flush();

            expectReply(reader, 250);

            sendCommand(writer, "QUIT");
        }
    }

    private static void sendCommand(BufferedWriter writer, String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
    }

    private static void expectReply(BufferedReader reader, int expectedCode) throws IOException {
        String line = reader.readLine();
        if (line == null || !line.startsWith(String.valueOf(expectedCode))) {
            throw new IOException("SMTP error response: " + line);
        }
    }

    private static void readUntilFinished(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            // Continuation lines have '-' after 3-digit code e.g. 250-smtp...
            if (line.length() >= 4 && line.charAt(3) == ' ') break;
            if (line.length() < 4) break;
        }
    }

    // ── Professional, Purpose-Specific Email Template ────────────────────────

    private static String buildOtpHtml(String code, OtpPurposeDetails details) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>{{SUBJECT}}</title>
            </head>
            <body style="margin: 0; padding: 30px 15px; background-color: #f4f6f8; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
              <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
                <tr>
                  <td align="center">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 580px; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 18px rgba(0, 0, 0, 0.06); padding: 40px 35px 35px 35px; border: 1px solid #e5e7eb;">
                      <tr>
                        <td align="center">
                          <!-- Purpose Badge -->
                          <div style="display: inline-block; background-color: #e0f2fe; color: #0284c7; font-size: 11px; font-weight: 800; letter-spacing: 1.2px; padding: 6px 16px; border-radius: 20px; text-transform: uppercase; margin-bottom: 18px;">
                            {{BADGE_TEXT}}
                          </div>
                          
                          <h1 style="margin: 0 0 14px 0; font-size: 24px; font-weight: 700; color: #111827; letter-spacing: -0.3px;">
                            {{HEADER_TITLE}}
                          </h1>
                          
                          <p style="margin: 0 0 28px 0; font-size: 14.5px; color: #4b5563; line-height: 1.6; text-align: left;">
                            {{DESCRIPTION_TEXT}}
                          </p>
                          
                          <!-- Blue OTP Code Box matching standard presentation -->
                          <table role="presentation" border="0" cellpadding="0" cellspacing="0" style="margin: 0 auto 24px auto;">
                            <tr>
                              <td align="center" style="background-color: #007bff; border-radius: 8px; padding: 16px 44px; box-shadow: 0 4px 14px rgba(0, 123, 255, 0.35);">
                                <span style="font-family: 'Consolas', 'Roboto Mono', 'Courier New', monospace; font-size: 36px; font-weight: 800; color: #ffffff; letter-spacing: 8px; display: inline-block;">
                                  {{OTP_CODE}}
                                </span>
                              </td>
                            </tr>
                          </table>

                          <p style="margin: 0 0 22px 0; font-size: 13.5px; color: #4b5563; font-weight: 600;">
                            ⏱ This One-Time Password is valid for <strong>5 minutes</strong> only.
                          </p>

                          <!-- Security Advisory Callout Box -->
                          <div style="background-color: #f8fafc; border-left: 4px solid #3b82f6; padding: 12px 16px; border-radius: 4px; text-align: left; margin-bottom: 24px;">
                            <p style="margin: 0; font-size: 12.5px; color: #475569; line-height: 1.5;">
                              <strong>🔒 Security Advisory:</strong> {{SECURITY_NOTICE}} Acads Catch Up staff will never request your verification code.
                            </p>
                          </div>

                          <hr style="border: 0; border-top: 1px solid #e5e7eb; margin: 0 0 20px 0;">

                          <p style="margin: 0 0 8px 0; font-size: 12px; color: #9ca3af;">
                            This is an automated administrative email from the Acads Catch Up Academic Portal. Please do not reply.
                          </p>

                          <p style="margin: 0; font-size: 11px; color: #94a3b8;">
                            Acads Catch Up • Developed by: <span style="color: #007bff; font-weight: 600;">F4TAL</span> • © 2026 All rights reserved.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
        return template
                .replace("{{SUBJECT}}", details.subject())
                .replace("{{BADGE_TEXT}}", details.badgeText())
                .replace("{{HEADER_TITLE}}", details.headerTitle())
                .replace("{{DESCRIPTION_TEXT}}", details.descriptionText())
                .replace("{{OTP_CODE}}", code)
                .replace("{{SECURITY_NOTICE}}", details.securityNotice());
    }
}
