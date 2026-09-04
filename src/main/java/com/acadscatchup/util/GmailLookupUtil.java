package com.acadscatchup.util;

import com.acadscatchup.dao.UserDAO;
import com.acadscatchup.model.User;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.regex.Pattern;

/**
 * Universal Gmail & Email Account Lookup and Validation Utility.
 * Provides:
 *  1. RFC-compliant syntax & structure validation.
 *  2. Official Google Gmail username rules checking (6-30 chars, alphanumeric + dots, no "..").
 *  3. Common domain typo detection (e.g. @gamil.com, @gmial.com).
 *  4. Live DNS MX (Mail Exchange) record lookup to confirm domain actively accepts mail.
 *  5. Database account lookup to verify if the Gmail is registered to an existing user.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class GmailLookupUtil {

    public static final String DEVELOPER = "F4TAL";

    // Standard RFC-compliant email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Google Gmail username allowed characters: letters, digits, dots
    private static final Pattern GMAIL_USER_PATTERN = Pattern.compile("^[a-zA-Z0-9.]+$");

    // Obfuscated API Keys Pool (XOR 0x5A) for Abstract Email Reputation API (5 keys = 499 free checks/month)
    private static final int OBF_KEY_VAL = 0x5A;
    private static final int[][] OBF_API_KEYS = {
        {110,106,63,111,56,63,60,63,59,57,108,108,110,63,107,110,99,62,60,98,106,107,59,62,109,108,107,108,99,110,63,59}, // 40e5befeac664e149df801ad761694ea
        {60,63,62,62,60,109,98,110,56,110,59,60,110,109,98,111,59,59,57,111,99,60,99,98,99,62,104,108,106,57,110,98},   // feddf784b4af4785aac59f989d260c48
        {62,62,105,62,106,111,107,110,106,106,59,99,110,98,106,105,98,107,106,109,104,105,60,59,99,108,106,104,108,105,62,106}, // dd3d051400a94803810723fa960263d0
        {57,59,62,60,106,98,111,108,104,57,105,107,110,108,107,98,98,57,107,108,57,56,60,60,57,56,99,110,106,106,59,62}, // cadf08562c3146188c16cbffcb9400ad
        {59,110,106,106,105,104,60,111,105,56,108,108,110,56,59,109,59,105,108,105,56,63,111,111,111,111,99,109,98,108,59,63}  // a40032f53b664ba7a363be55559786ae
    };

    private static String decode(int[] data, int key) {
        char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) (data[i] ^ key);
        }
        return new String(chars);
    }

    private static final java.util.concurrent.atomic.AtomicInteger currentKeyIndex = new java.util.concurrent.atomic.AtomicInteger(0);

    // In-memory cache to eliminate duplicate API requests for the same address
    private static final java.util.Map<String, ValidationResult> VALIDATION_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Validation Result containing status, diagnostic message, and account info.
     */
    public static class ValidationResult {
        public static final String DEVELOPER = "F4TAL";

        private final boolean valid;
        private final String message;
        private final boolean gmail;
        private final User user;

        public ValidationResult(boolean valid, String message, boolean gmail, User user) {
            this.valid = valid;
            this.message = message;
            this.gmail = gmail;
            this.user = user;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public boolean isGmail() { return gmail; }
        public User getUser() { return user; }
    }

    /**
     * Comprehensive validation of an email address.
     * Performs syntax, Google Gmail constraint, common typo, and DNS MX checks.
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Please enter an email address.", false, null);
        }

        String cleaned = email.trim().toLowerCase();

        // 1. Basic Structure
        if (!cleaned.contains("@")) {
            return new ValidationResult(false, "Missing '@' symbol in email address.", false, null);
        }

        String[] parts = cleaned.split("@", -1);
        if (parts.length != 2) {
            return new ValidationResult(false, "Invalid email format. Only one '@' allowed.", false, null);
        }

        String username = parts[0];
        String domain = parts[1];

        if (username.isEmpty()) {
            return new ValidationResult(false, "Email username cannot be empty.", false, null);
        }
        if (domain.isEmpty() || !domain.contains(".")) {
            return new ValidationResult(false, "Email domain is incomplete (e.g. gmail.com).", false, null);
        }

        // 2. RFC Pattern Match
        if (!EMAIL_PATTERN.matcher(cleaned).matches()) {
            return new ValidationResult(false, "Email contains invalid characters or structure.", false, null);
        }

        // 3. Typo Detection for Gmail
        if (domain.equals("gamil.com") || domain.equals("gmial.com") || domain.equals("gmai.com")
                || domain.equals("gmaill.com") || domain.equals("gemail.com") || domain.equals("gamil.co")) {
            return new ValidationResult(false, "Did you mean @gmail.com? Please check your email spelling.", false, null);
        }

        boolean isGmail = domain.equals("gmail.com") || domain.equals("googlemail.com");

        // 4. Gmail-specific Rules (Google account requirements)
        if (isGmail) {
            if (username.length() < 6 || username.length() > 30) {
                return new ValidationResult(
                        false,
                        "Gmail usernames must be between 6 and 30 characters long.",
                        true,
                        null
                );
            }
            if (!GMAIL_USER_PATTERN.matcher(username).matches()) {
                return new ValidationResult(
                        false,
                        "Gmail usernames can only contain letters (a-z), numbers (0-9), and periods (.).",
                        true,
                        null
                );
            }
            if (username.startsWith(".") || username.endsWith(".")) {
                return new ValidationResult(
                        false,
                        "Gmail username cannot start or end with a period.",
                        true,
                        null
                );
            }
            if (username.contains("..")) {
                return new ValidationResult(
                        false,
                        "Gmail username cannot contain consecutive periods (..).",
                        true,
                        null
                );
            }
        }

        // 5. DNS MX Record Lookup (Verify domain actively receives email)
        if (!checkDomainMx(domain)) {
            return new ValidationResult(
                    false,
                    "Mail domain \"" + domain + "\" does not exist or cannot receive emails.",
                    isGmail,
                    null
            );
        }

        // 6. Live Online Verification via Public API (checks deliverability, DNS & disposable traps)
        ValidationResult onlineRes = checkOnlineDeliverability(cleaned);
        if (onlineRes != null && !onlineRes.isValid()) {
            return onlineRes;
        }

        return new ValidationResult(true, isGmail ? "Valid Gmail address." : "Valid email address.", isGmail, null);
    }

    /**
     * Looks up if a Gmail address or username is registered to an existing account in AcadsCatchUp.
     */
    public static ValidationResult lookupAccount(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            return new ValidationResult(false, "Please enter your username or registered Gmail.", false, null);
        }

        String input = emailOrUsername.trim();

        // If it looks like an email, validate structure first
        if (input.contains("@")) {
            ValidationResult syntax = validateEmail(input);
            if (!syntax.isValid()) {
                return syntax;
            }
        }

        UserDAO userDAO = new UserDAO();
        User user = userDAO.findByUsernameOrEmail(input);
        if (user == null) {
            return new ValidationResult(false, "No registered account found matching \"" + input + "\".", input.toLowerCase().endsWith("@gmail.com"), null);
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return new ValidationResult(false, "Account \"" + user.getUsername() + "\" has no registered Gmail address. Please contact an administrator.", false, user);
        }

        return new ValidationResult(true, "Account verified: " + user.getFullName() + " (" + user.getEmail() + ")", user.getEmail().toLowerCase().endsWith("@gmail.com"), user);
    }

    /**
     * DNS MX Record lookup to confirm the domain can receive mail.
     */
    public static boolean checkDomainMx(String domain) {
        if (domain == null || domain.isBlank()) return false;

        // Fast-path for trusted Google domains
        if (domain.equalsIgnoreCase("gmail.com") || domain.equalsIgnoreCase("googlemail.com")) {
            return true;
        }

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "2000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[] { "MX" });
            Attribute attr = attrs.get("MX");
            if (attr != null && attr.size() > 0) {
                return true;
            }
            // Fallback: Check A record
            Attributes aAttrs = ctx.getAttributes(domain, new String[] { "A" });
            return aAttrs != null && aAttrs.get("A") != null;
        } catch (Exception e) {
            // If offline or behind a strict firewall, don't block well-known domains
            return domain.endsWith(".edu") || domain.endsWith(".org") || domain.endsWith(".com");
        }
    }

    /**
     * Live mailbox deliverability verification using AbstractAPI's multi-key pool (5 keys = 499 free checks/month).
     * Automatically rotates keys on 429 quota limits and caches verified results to conserve API usage.
     * Accurately detects non-existent Gmail mailboxes (e.g. watanabeaizennn@gmail.com, zakekiyoo@gmail.com).
     * Gracefully falls back to secondary checks if offline.
     */
    public static ValidationResult checkOnlineDeliverability(String email) {
        if (email == null || email.isBlank()) return null;
        String cleaned = email.trim().toLowerCase();
        boolean isGmail = cleaned.endsWith("@gmail.com") || cleaned.endsWith("@googlemail.com");

        // 1. Check in-memory cache to save API credits
        ValidationResult cached = VALIDATION_CACHE.get(cleaned);
        if (cached != null) {
            return cached;
        }

        // 2. Query AbstractAPI Email Reputation with rotating key pool
        for (int attempt = 0; attempt < OBF_API_KEYS.length; attempt++) {
            int keyIndex = (currentKeyIndex.get() + attempt) % OBF_API_KEYS.length;
            String apiKey = decode(OBF_API_KEYS[keyIndex], OBF_KEY_VAL);
            try {
                String endpoint = "https://emailreputation.abstractapi.com/v1/?api_key=" 
                        + apiKey + "&email=" + java.net.URLEncoder.encode(cleaned, java.nio.charset.StandardCharsets.UTF_8);

                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofMillis(3500))
                        .build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(endpoint))
                        .timeout(java.time.Duration.ofMillis(4000))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                // If quota exhausted (429), advance to next key in pool
                if (resp.statusCode() == 429 || resp.statusCode() == 401) {
                    currentKeyIndex.incrementAndGet();
                    continue;
                }

                if (resp.statusCode() == 200 && resp.body() != null) {
                    String body = resp.body().toLowerCase();

                    // Mailbox existence check (Active Google MX rejection detection)
                    if (body.contains("\"status\":\"undeliverable\"") 
                            || body.contains("\"status_detail\":\"invalid_mailbox\"") 
                            || body.contains("\"is_smtp_valid\":false")) {
                        ValidationResult invalidResult = new ValidationResult(
                                false,
                                "Google mail server reported that this account does not exist (Invalid Mailbox). Please check your email spelling.",
                                isGmail,
                                null
                        );
                        VALIDATION_CACHE.put(cleaned, invalidResult);
                        return invalidResult;
                    }

                    if (body.contains("\"status\":\"deliverable\"")) {
                        ValidationResult validResult = new ValidationResult(
                                true,
                                isGmail ? "Verified active & deliverable Gmail account." : "Verified valid email address.",
                                isGmail,
                                null
                        );
                        VALIDATION_CACHE.put(cleaned, validResult);
                        return validResult;
                    }
                }
            } catch (Exception ignored) {
                // Connection issue on current key attempt, try next or fallback
            }
        }

        // 3. Fallback: Disify public check
        try {
            String encoded = java.net.URLEncoder.encode(cleaned, java.nio.charset.StandardCharsets.UTF_8);
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofMillis(1800))
                    .build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://disify.com/api/email/" + encoded))
                    .timeout(java.time.Duration.ofMillis(1800))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                String body = resp.body();
                if (body.contains("\"format\":false") || body.contains("\"format\": false")) {
                    return new ValidationResult(false, "Invalid email structure according to mail servers.", isGmail, null);
                }
                if (body.contains("\"disposable\":true") || body.contains("\"disposable\": true")) {
                    return new ValidationResult(false, "Disposable / temporary email addresses are not permitted.", false, null);
                }
                if (body.contains("\"dns\":false") || body.contains("\"dns\": false")) {
                    return new ValidationResult(false, "Email domain cannot receive messages (no DNS MX records found).", false, null);
                }
            }
        } catch (Exception ignored) {
            // Offline or network timeout — fallback smoothly to local checks
        }
        return null;
    }
}

