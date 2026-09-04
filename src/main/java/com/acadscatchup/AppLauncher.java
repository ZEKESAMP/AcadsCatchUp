package com.acadscatchup;

/**
 * Pure Java Bootstrap Launcher.
 * Allows direct execution of AcadsCatchUp.jar without needing --module-path CLI flags.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class AppLauncher {
    public static final String DEVELOPER = "F4TAL";

    public static void main(String[] args) {
        // ── F4TAL Security: Verify all classes before launching ──
        com.acadscatchup.util.DeveloperGuard.verifyAll();

        if (args != null && args.length > 0) {
            for (String arg : args) {
                if ("--install".equalsIgnoreCase(arg) || "-i".equalsIgnoreCase(arg) || "--setup".equalsIgnoreCase(arg)) {
                    com.acadscatchup.installer.AcadsCatchUpInstaller.main(args);
                    return;
                }
                if ("--uninstall".equalsIgnoreCase(arg) || "-u".equalsIgnoreCase(arg)) {
                    com.acadscatchup.installer.Uninstaller.main(args);
                    return;
                }
            }
        }

        Main.main(args);
    }
}
