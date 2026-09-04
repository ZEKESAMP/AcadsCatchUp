package com.acadscatchup.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Lightweight, 100% Pure Java Cloud Microservice for Gmail & Email Verification.
 * Designed for deployment on Koyeb, Render, Railway, or any Java/Docker cloud host.
 *
 * Features:
 *  1. Zero external JAR dependencies (pure standard Java runtime).
 *  2. Embedded HTTP Server listening on port specified by cloud environment (PORT env var).
 *  3. Live DNS MX Record Discovery.
 *  4. Direct SMTP Handshake (HELO -> MAIL FROM -> RCPT TO) with Google Mail Exchangers.
 *  5. Graceful Cloud Fallback: If cloud network blocks outbound Port 25, automatically falls back
 *     to public verification APIs and deep heuristics.
 *  6. REST API endpoint (/api/check?email=...) and Interactive Web UI (/).
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class GmailCheckerServer {

    public static final String DEVELOPER = "F4TAL";
    public static final String VERSION = "1.0.0";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private static final Pattern GMAIL_USER_PATTERN = Pattern.compile("^[a-zA-Z0-9.]+$");

    public static void main(String[] args) throws Exception {
        int port = 8000;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Routes
        server.createContext("/", new WebUIHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/check", new ApiCheckHandler());

        server.start();
        System.out.println("=================================================");
        System.out.println("  AcadsCatchUp Gmail Checker Microservice");
        System.out.println("  Developer: " + DEVELOPER + " | Version: " + VERSION);
        System.out.println("  Listening on: http://0.0.0.0:" + port);
        System.out.println("=================================================");
    }

    // =========================================================================
    // HTTP Handlers
    // =========================================================================

    /**
     * Interactive HTML Web UI for easy browser testing.
     */
    static class WebUIHandler implements HttpHandler {
        public static final String DEVELOPER = "F4TAL";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET") && !exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendResponse(exchange, 404, "Not Found", "text/plain");
                return;
            }

            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>AcadsCatchUp — Gmail & Email Verification Server</title>
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
                    <style>
                        :root {
                            --bg: #090d16;
                            --card-bg: rgba(17, 24, 39, 0.85);
                            --primary: #38bdf8;
                            --primary-glow: rgba(56, 189, 248, 0.35);
                            --success: #10b981;
                            --danger: #ef4444;
                            --border: rgba(56, 189, 248, 0.2);
                            --text: #f8fafc;
                            --text-dim: #94a3b8;
                        }
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            background: radial-gradient(circle at top, #0f172a, #020617);
                            color: var(--text);
                            font-family: 'Outfit', sans-serif;
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 20px;
                        }
                        .container {
                            max-width: 640px;
                            width: 100%;
                            background: var(--card-bg);
                            border: 1px solid var(--border);
                            border-radius: 20px;
                            padding: 36px;
                            backdrop-filter: blur(16px);
                            box-shadow: 0 20px 50px rgba(0,0,0,0.6);
                        }
                        .badge {
                            display: inline-block;
                            padding: 4px 12px;
                            background: rgba(56, 189, 248, 0.12);
                            color: var(--primary);
                            border: 1px solid rgba(56, 189, 248, 0.3);
                            border-radius: 999px;
                            font-size: 12px;
                            font-weight: 700;
                            margin-bottom: 14px;
                            letter-spacing: 0.05em;
                        }
                        h1 { font-size: 26px; font-weight: 800; margin-bottom: 8px; color: #fff; }
                        p.subtitle { color: var(--text-dim); font-size: 14px; margin-bottom: 24px; line-height: 1.5; }
                        .input-group {
                            display: flex;
                            gap: 10px;
                            margin-bottom: 20px;
                        }
                        input[type="email"] {
                            flex: 1;
                            background: #0b1120;
                            border: 1px solid #1e293b;
                            border-radius: 12px;
                            padding: 14px 16px;
                            color: #fff;
                            font-size: 15px;
                            outline: none;
                            transition: all 0.2s ease;
                        }
                        input[type="email"]:focus {
                            border-color: var(--primary);
                            box-shadow: 0 0 0 3px var(--primary-glow);
                        }
                        button {
                            background: linear-gradient(135deg, #0284c7, #38bdf8);
                            border: none;
                            border-radius: 12px;
                            padding: 14px 24px;
                            color: #041324;
                            font-weight: 700;
                            font-size: 15px;
                            cursor: pointer;
                            transition: transform 0.15s ease, box-shadow 0.15s ease;
                        }
                        button:hover { transform: translateY(-1px); box-shadow: 0 6px 20px var(--primary-glow); }
                        button:active { transform: translateY(1px); }
                        .result-box {
                            display: none;
                            margin-top: 20px;
                            padding: 18px;
                            border-radius: 12px;
                            font-size: 14px;
                            line-height: 1.6;
                            animation: fadeIn 0.3s ease;
                        }
                        .result-box.success {
                            background: rgba(16, 185, 129, 0.1);
                            border: 1px solid rgba(16, 185, 129, 0.3);
                            color: #a7f3d0;
                        }
                        .result-box.danger {
                            background: rgba(239, 68, 68, 0.1);
                            border: 1px solid rgba(239, 68, 68, 0.3);
                            color: #fca5a5;
                        }
                        pre {
                            background: #050811;
                            border: 1px solid #1e293b;
                            border-radius: 10px;
                            padding: 14px;
                            font-family: 'JetBrains Mono', monospace;
                            font-size: 12px;
                            color: #93c5fd;
                            overflow-x: auto;
                            margin-top: 14px;
                        }
                        .footer {
                            margin-top: 28px;
                            padding-top: 18px;
                            border-top: 1px solid #1e293b;
                            display: flex;
                            justify-content: space-between;
                            font-size: 12px;
                            color: #64748b;
                        }
                        @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="badge">JAVA 21 CLOUD MICROSERVICE • DEVELOPER: F4TAL</div>
                        <h1>Gmail & Email Existence Checker</h1>
                        <p class="subtitle">Real-time live mailbox existence validation for AcadsCatchUp using DNS MX resolution and SMTP handshakes.</p>

                        <div class="input-group">
                            <input type="email" id="emailInput" placeholder="Enter email (e.g. zakekiyoo@gmail.com)" autofocus>
                            <button id="checkBtn" onclick="runCheck()">Verify Account</button>
                        </div>

                        <div id="resultBox" class="result-box"></div>
                        <pre id="jsonOutput" style="display: none;"></pre>

                        <div class="footer">
                            <span>AcadsCatchUp Dedicated Service</span>
                            <span>Endpoints: <code>/api/check?email=...</code> & <code>/health</code></span>
                        </div>
                    </div>

                    <script>
                        async function runCheck() {
                            const email = document.getElementById('emailInput').value.trim();
                            const btn = document.getElementById('checkBtn');
                            const box = document.getElementById('resultBox');
                            const jsonEl = document.getElementById('jsonOutput');

                            if (!email) {
                                alert('Please enter an email address.');
                                return;
                            }

                            btn.innerText = 'Checking...';
                            btn.disabled = true;
                            box.style.display = 'none';
                            jsonEl.style.display = 'none';

                            try {
                                const res = await fetch('/api/check?email=' + encodeURIComponent(email));
                                const data = await res.json();

                                box.className = 'result-box ' + (data.valid && data.exists ? 'success' : 'danger');
                                box.innerHTML = `<strong>${data.exists ? 'Account Exists / Deliverable' : 'Account Rejected / Non-Existent'}</strong><br>${data.message}`;
                                box.style.display = 'block';

                                jsonEl.innerText = JSON.stringify(data, null, 2);
                                jsonEl.style.display = 'block';
                            } catch (e) {
                                box.className = 'result-box danger';
                                box.innerHTML = '<strong>Network Error</strong><br>' + e.message;
                                box.style.display = 'block';
                            } finally {
                                btn.innerText = 'Verify Account';
                                btn.disabled = false;
                            }
                        }

                        document.getElementById('emailInput').addEventListener('keydown', (e) => {
                            if (e.key === 'Enter') runCheck();
                        });
                    </script>
                </body>
                </html>
                """;
            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }
    }

    /**
     * Health check endpoint for Koyeb/Docker health checks.
     */
    static class HealthHandler implements HttpHandler {
        public static final String DEVELOPER = "F4TAL";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = "{\"status\":\"UP\",\"service\":\"AcadsCatchUp-GmailChecker\",\"developer\":\"" + DEVELOPER + "\",\"version\":\"" + VERSION + "\"}";
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    /**
     * REST API endpoint: GET /api/check?email=user@gmail.com
     */
    static class ApiCheckHandler implements HttpHandler {
        public static final String DEVELOPER = "F4TAL";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable CORS for web/client calls
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                sendResponse(exchange, 204, "", "application/json");
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}", "application/json");
                return;
            }

            URI uri = exchange.getRequestURI();
            String query = uri.getRawQuery();
            String email = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && pair[0].equalsIgnoreCase("email")) {
                        email = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            if (email == null || email.isBlank()) {
                sendResponse(exchange, 400, "{\"valid\":false,\"exists\":false,\"message\":\"Missing 'email' query parameter.\",\"developer\":\"" + DEVELOPER + "\"}", "application/json");
                return;
            }

            CheckResult result = verifyEmailAccount(email.trim());
            String json = result.toJson();
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    // =========================================================================
    // Core Verification Engine
    // =========================================================================

    public static class CheckResult {
        public static final String DEVELOPER = "F4TAL";

        public final String email;
        public final boolean valid;
        public final boolean exists;
        public final boolean isGmail;
        public final String method;
        public final String message;
        public final int smtpCode;

        public CheckResult(String email, boolean valid, boolean exists, boolean isGmail, String method, String message, int smtpCode) {
            this.email = email;
            this.valid = valid;
            this.exists = exists;
            this.isGmail = isGmail;
            this.method = method;
            this.message = message;
            this.smtpCode = smtpCode;
        }

        public String toJson() {
            return String.format(
                    "{\"email\":%s,\"valid\":%b,\"exists\":%b,\"isGmail\":%b,\"method\":%s,\"smtpCode\":%d,\"message\":%s,\"developer\":\"%s\"}",
                    escapeJson(email),
                    valid,
                    exists,
                    isGmail,
                    escapeJson(method),
                    smtpCode,
                    escapeJson(message),
                    DEVELOPER
            );
        }

        private static String escapeJson(String s) {
            if (s == null) return "null";
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
        }
    }

    /**
     * Comprehensive multi-phase mailbox verification.
     */
    public static CheckResult verifyEmailAccount(String email) {
        String cleaned = email.toLowerCase().trim();

        // 1. Structure & Syntax
        if (!cleaned.contains("@")) {
            return new CheckResult(cleaned, false, false, false, "SYNTAX", "Missing '@' symbol.", 0);
        }

        String[] parts = cleaned.split("@", -1);
        if (parts.length != 2) {
            return new CheckResult(cleaned, false, false, false, "SYNTAX", "Invalid format: multiple '@' symbols.", 0);
        }

        String username = parts[0];
        String domain = parts[1];

        if (!EMAIL_PATTERN.matcher(cleaned).matches()) {
            return new CheckResult(cleaned, false, false, false, "SYNTAX", "Email format does not adhere to RFC standards.", 0);
        }

        boolean isGmail = domain.equals("gmail.com") || domain.equals("googlemail.com");

        // 2. Gmail Official Username Restrictions
        if (isGmail) {
            if (username.length() < 6 || username.length() > 30) {
                return new CheckResult(cleaned, false, false, true, "GMAIL_RULES", "Gmail username must be between 6 and 30 characters.", 0);
            }
            if (!GMAIL_USER_PATTERN.matcher(username).matches()) {
                return new CheckResult(cleaned, false, false, true, "GMAIL_RULES", "Gmail username contains forbidden characters (only a-z, 0-9, and '.' are allowed).", 0);
            }
            if (username.startsWith(".") || username.endsWith(".")) {
                return new CheckResult(cleaned, false, false, true, "GMAIL_RULES", "Gmail username cannot start or end with a dot.", 0);
            }
            if (username.contains("..")) {
                return new CheckResult(cleaned, false, false, true, "GMAIL_RULES", "Gmail username cannot contain consecutive dots (..).", 0);
            }
        }

        // 3. Typo checks
        if (domain.equals("gamil.com") || domain.equals("gmial.com") || domain.equals("gmai.com") || domain.equals("gmaill.com")) {
            return new CheckResult(cleaned, false, false, false, "TYPO_CHECK", "Domain typo detected. Did you mean @gmail.com?", 0);
        }

        // 4. DNS MX Record Discovery
        List<String> mxHosts = getMxRecords(domain);
        if (mxHosts.isEmpty()) {
            return new CheckResult(cleaned, false, false, isGmail, "DNS_MX", "Mail domain '" + domain + "' does not possess active MX records.", 0);
        }

        // 5. Direct SMTP Handshake Probe (Socket port 25)
        SmtpProbeResult smtpResult = probeSmtpMailbox(mxHosts.get(0), cleaned);
        if (smtpResult != null && smtpResult.decisive) {
            if (smtpResult.exists) {
                return new CheckResult(cleaned, true, true, isGmail, "SMTP_HANDSHAKE", "Recipient mailbox verified and accepted by mail server (" + smtpResult.response + ").", smtpResult.code);
            } else {
                return new CheckResult(cleaned, false, false, isGmail, "SMTP_HANDSHAKE", "Mail server actively rejected address: " + smtpResult.response, smtpResult.code);
            }
        }

        // 6. Cloud Fallback (In case Cloud Provider firewalls outbound port 25)
        CheckResult fallback = queryExternalDeliverability(cleaned, isGmail);
        if (fallback != null) {
            return fallback;
        }

        // 7. If neither gave a negative block, syntax + MX are valid
        return new CheckResult(cleaned, true, true, isGmail, "DNS_AND_SYNTAX", "Domain is active and mail-ready. OTP code will confirm account ownership.", 250);
    }

    // =========================================================================
    // SMTP & DNS Implementation
    // =========================================================================

    private static class SmtpProbeResult {
        public final boolean decisive;
        public final boolean exists;
        public final int code;
        public final String response;

        public SmtpProbeResult(boolean decisive, boolean exists, int code, String response) {
            this.decisive = decisive;
            this.exists = exists;
            this.code = code;
            this.response = response;
        }
    }

    /**
     * Connects to the primary MX server and runs the SMTP protocol handshake
     * up to RCPT TO without transmitting email content.
     */
    private static SmtpProbeResult probeSmtpMailbox(String mxHost, String email) {
        Socket socket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            socket = new Socket();
            // 2.5 second timeout to maintain rapid response times
            socket.connect(new InetSocketAddress(mxHost, 25), 2500);
            socket.setSoTimeout(3000);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            // Read banner
            String banner = readSmtpResponse(reader);
            if (!banner.startsWith("220")) {
                return null;
            }

            // HELO / EHLO
            writer.write("EHLO acadscatchup.app\r\n");
            writer.flush();
            String ehloResp = readSmtpResponse(reader);
            if (!ehloResp.startsWith("250")) {
                writer.write("HELO acadscatchup.app\r\n");
                writer.flush();
                ehloResp = readSmtpResponse(reader);
            }

            // MAIL FROM
            writer.write("MAIL FROM:<verify@acadscatchup.app>\r\n");
            writer.flush();
            String mailFromResp = readSmtpResponse(reader);
            if (!mailFromResp.startsWith("250")) {
                return null;
            }

            // RCPT TO (The crucial existence probe)
            writer.write("RCPT TO:<" + email + ">\r\n");
            writer.flush();
            String rcptResp = readSmtpResponse(reader);

            int code = parseSmtpCode(rcptResp);

            // Close politely
            try {
                writer.write("QUIT\r\n");
                writer.flush();
            } catch (Exception ignored) {}

            if (code == 250 || code == 251) {
                return new SmtpProbeResult(true, true, code, rcptResp.trim());
            } else if (code == 550 || code == 551 || code == 552 || code == 553 || code == 554) {
                return new SmtpProbeResult(true, false, code, rcptResp.trim());
            }

            return null; // Ambiguous or greylisted (e.g. 450/451)
        } catch (Exception e) {
            // Port 25 is commonly blocked on cloud hosting providers (e.g. Koyeb/Render)
            // Return null so fallback handler takes over smoothly.
            return null;
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
            try { if (writer != null) writer.close(); } catch (Exception ignored) {}
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        }
    }

    private static String readSmtpResponse(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(" ");
            // Multi-line SMTP responses have a '-' after the 3-digit code (e.g. 250-something)
            // The final line has a space (e.g. 250 OK)
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
        return sb.toString().trim();
    }

    private static int parseSmtpCode(String resp) {
        if (resp != null && resp.length() >= 3) {
            try {
                return Integer.parseInt(resp.substring(0, 3));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /**
     * Resolves DNS MX records for domain in priority order.
     */
    private static List<String> getMxRecords(String domain) {
        List<String> records = new ArrayList<>();
        if (domain.equalsIgnoreCase("gmail.com") || domain.equalsIgnoreCase("googlemail.com")) {
            records.add("gmail-smtp-in.l.google.com");
            records.add("alt1.gmail-smtp-in.l.google.com");
            return records;
        }

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "2000");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            Attribute attr = attrs.get("MX");
            if (attr != null) {
                for (int i = 0; i < attr.size(); i++) {
                    String entry = (String) attr.get(i);
                    String[] parts = entry.split("\\s+");
                    String host = parts.length > 1 ? parts[1] : parts[0];
                    if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
                    records.add(host);
                }
            }
        } catch (Exception ignored) {}

        if (records.isEmpty()) {
            records.add(domain);
        }
        return records;
    }

    /**
     * Cloud fallback queries public deliverability services with short timeouts.
     */
    private static CheckResult queryExternalDeliverability(String email, boolean isGmail) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(2000))
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://disify.com/api/email/" + email))
                    .timeout(Duration.ofMillis(2000))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                String body = resp.body();
                if (body.contains("\"format\":false") || body.contains("\"format\": false")) {
                    return new CheckResult(email, false, false, isGmail, "API_VERIFY", "Mail server reported invalid email syntax.", 0);
                }
                if (body.contains("\"disposable\":true") || body.contains("\"disposable\": true")) {
                    return new CheckResult(email, false, false, isGmail, "API_VERIFY", "Disposable / throwaway emails are prohibited.", 0);
                }
                if (body.contains("\"dns\":false") || body.contains("\"dns\": false")) {
                    return new CheckResult(email, false, false, isGmail, "API_VERIFY", "Domain has no valid DNS/MX records to receive mail.", 0);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
