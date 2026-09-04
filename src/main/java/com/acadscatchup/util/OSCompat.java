package com.acadscatchup.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cross-Platform OS Compatibility Utility.
 * Detects the host operating system and provides emoji-to-text fallback
 * for Linux environments where color emoji fonts (e.g. Segoe UI Emoji,
 * Apple Color Emoji) are unavailable, causing glyphs to render as blank
 * squares ("tofu"). On Windows and macOS, all emoji render natively.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class OSCompat {

    public static final String DEVELOPER = "F4TAL";

    public enum OS { WINDOWS, LINUX, MAC, UNKNOWN }

    private static OS detectedOS = null;

    /** Emoji → plain-text fallback map for Linux. */
    private static final Map<String, String> EMOJI_FALLBACK = new LinkedHashMap<>();

    static {
        // ── Action Buttons ──
        EMOJI_FALLBACK.put("\uD83C\uDF93", "[Enroll]");   // 🎓
        EMOJI_FALLBACK.put("\uD83D\uDCBE", "[Save]");     // 💾
        EMOJI_FALLBACK.put("\uD83D\uDDD1", "[Del]");      // 🗑
        EMOJI_FALLBACK.put("\uD83D\uDCA5", "[Bulk]");     // 💥
        EMOJI_FALLBACK.put("\uD83D\uDCE4", "[Submit]");   // 📤
        EMOJI_FALLBACK.put("\uD83D\uDCE5", "[Inbox]");    // 📥
        EMOJI_FALLBACK.put("\uD83D\uDCEC", "[Mail]");     // 📬
        EMOJI_FALLBACK.put("\uD83D\uDCCE", "[Attach]");   // 📎
        EMOJI_FALLBACK.put("\uD83D\uDCC2", "[File]");     // 📂
        EMOJI_FALLBACK.put("\uD83D\uDCC4", "[Doc]");      // 📄
        EMOJI_FALLBACK.put("\uD83D\uDCDA", "[Books]");    // 📚
        EMOJI_FALLBACK.put("\uD83D\uDCAC", "[Chat]");     // 💬
        EMOJI_FALLBACK.put("\uD83D\uDC65", "[Users]");    // 👥
        EMOJI_FALLBACK.put("\uD83D\uDC64", "[User]");     // 👤

        // ── Status / Indicators ──
        EMOJI_FALLBACK.put("\uD83D\uDFE2", "[ON]");       // 🟢
        EMOJI_FALLBACK.put("\uD83D\uDD04", "[Sync]");     // 🔄
        EMOJI_FALLBACK.put("\uD83D\uDFE0", "[WARN]");     // 🟠
        EMOJI_FALLBACK.put("\uD83D\uDD34", "[OFF]");      // 🔴
        EMOJI_FALLBACK.put("⭐", "[*]");
        EMOJI_FALLBACK.put("★", "[*]");

        // ── Symbols ──
        EMOJI_FALLBACK.put("✏", "[Edit]");
        EMOJI_FALLBACK.put("✔", "[OK]");
        EMOJI_FALLBACK.put("✉", "[Send]");
        EMOJI_FALLBACK.put("❌", "[X]");
        EMOJI_FALLBACK.put("❓", "[?]");
        EMOJI_FALLBACK.put("⚙", "[Settings]");
        EMOJI_FALLBACK.put("＋", "+");
        EMOJI_FALLBACK.put("➔", "->");

        // ── Info / Misc ──
        EMOJI_FALLBACK.put("\uD83D\uDCA1", "[Tip]");      // 💡
        EMOJI_FALLBACK.put("\uD83D\uDCBB", "[PC]");       // 💻
        EMOJI_FALLBACK.put("\uD83D\uDE80", "[Launch]");   // 🚀
        EMOJI_FALLBACK.put("\uD83D\uDEAA", "[Door]");     // 🚪
        EMOJI_FALLBACK.put("\uD83D\uDD10", "[Lock]");     // 🔐
        EMOJI_FALLBACK.put("\uD83D\uDD11", "[Key]");      // 🔑
        EMOJI_FALLBACK.put("\uD83D\uDD12", "[Lock]");     // 🔒
        EMOJI_FALLBACK.put("\uD83D\uDD13", "[Unlock]");   // 🔓
        EMOJI_FALLBACK.put("\uD83D\uDCD6", "[Book]");     // 📖
        EMOJI_FALLBACK.put("\u26A1", "[!]");              // ⚡
        EMOJI_FALLBACK.put("\uD83D\uDC68\u200D\uD83C\uDFEB", "[Prof]"); // 👨‍🏫
        EMOJI_FALLBACK.put("\uD83D\uDD0D", "[Search]");   // 🔍
        EMOJI_FALLBACK.put("\uD83C\uDF89", "*");          // 🎉
        EMOJI_FALLBACK.put("\uD83D\uDEE1\uFE0F", "[Shield]"); // 🛡️
        EMOJI_FALLBACK.put("\uD83D\uDEE1", "[Shield]");   // 🛡
        EMOJI_FALLBACK.put("\uD83D\uDCCB", "[List]");     // 📋
        EMOJI_FALLBACK.put("\uD83D\uDCC5", "[Date]");     // 📅
        EMOJI_FALLBACK.put("\uD83D\uDCCA", "[Stats]");    // 📊
        EMOJI_FALLBACK.put("\u2728", "*");                // ✨
        EMOJI_FALLBACK.put("\uD83C\uDFF7\uFE0F", "[Tag]"); // 🏷️
        EMOJI_FALLBACK.put("\uD83C\uDFF7", "[Tag]");      // 🏷
        EMOJI_FALLBACK.put("ℹ️", "[i]");
        EMOJI_FALLBACK.put("ℹ", "[i]");
        EMOJI_FALLBACK.put("⚠️", "[!]");
        EMOJI_FALLBACK.put("⚠", "[!]");
        EMOJI_FALLBACK.put("©", "(c)");
    }

    /**
     * Initializes OS detection. Call once at application startup.
     */
    public static void init() {
        detectOS();
        if (isLinux()) {
            System.out.println("[OSCompat] Linux detected — emoji text fallback enabled, font fallback active.");
        } else if (isMac()) {
            System.out.println("[OSCompat] macOS detected — native emoji rendering.");
        } else {
            System.out.println("[OSCompat] Windows detected — native emoji rendering.");
        }
    }

    private static OS detectOS() {
        if (detectedOS != null) return detectedOS;
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            detectedOS = OS.WINDOWS;
        } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
            detectedOS = OS.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            detectedOS = OS.MAC;
        } else {
            detectedOS = OS.UNKNOWN;
        }
        return detectedOS;
    }

    public static OS getOS() {
        return detectOS();
    }

    public static boolean isWindows() { return detectOS() == OS.WINDOWS; }
    public static boolean isLinux()   { return detectOS() == OS.LINUX; }
    public static boolean isMac()     { return detectOS() == OS.MAC; }

    /**
     * Returns the recommended JavaFX font-family for the current OS.
     */
    public static String getRecommendedFontFamily() {
        return switch (detectOS()) {
            case WINDOWS -> "Segoe UI";
            case MAC     -> "SF Pro Text";
            case LINUX   -> "Noto Sans";
            default      -> "sans-serif";
        };
    }

    /**
     * Returns true if the AWT SystemTray is likely supported on the current platform.
     * On some Linux desktop environments (Wayland, tiling WMs), SystemTray is unsupported.
     */
    public static boolean isSystemTraySupported() {
        try {
            return java.awt.SystemTray.isSupported();
        } catch (Exception | UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Processes a string label and replaces emoji characters with text fallbacks
     * on Linux. On Windows/macOS the string is returned unchanged.
     */
    public static String label(String text) {
        if (text == null || text.isEmpty()) return text;
        if (!isLinux()) return text;

        String result = text;
        for (Map.Entry<String, String> entry : EMOJI_FALLBACK.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Recursively walks a JavaFX scene graph and replaces emoji text in all
     * Labels and Buttons with text fallbacks on Linux.
     * Call this once in a controller's initialize() method AFTER the FXML is loaded.
     */
    public static void patchEmojis(Parent root) {
        if (!isLinux() || root == null) return;
        patchNode(root);
    }

    private static void patchNode(Node node) {
        if (node instanceof Labeled labeled) {
            String text = labeled.getText();
            if (text != null && !text.isEmpty()) {
                String patched = label(text);
                if (!patched.equals(text)) {
                    labeled.setText(patched);
                }
            }
        }
        if (node instanceof javafx.scene.control.TextInputControl textInput) {
            String prompt = textInput.getPromptText();
            if (prompt != null && !prompt.isEmpty()) {
                String patched = label(prompt);
                if (!patched.equals(prompt)) {
                    textInput.setPromptText(patched);
                }
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                patchNode(child);
            }
        }
    }
}
