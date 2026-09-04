package com.acadscatchup.util;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * DeveloperGuard — Runtime integrity verification system.
 * Scans ALL classes in the com.acadscatchup package and verifies that each one
 * contains: public static final String DEVELOPER = "F4TAL"
 *
 * If any class is missing or has a tampered DEVELOPER field, the application
 * refuses to launch and throws a fatal RuntimeException.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class DeveloperGuard {

    public static final String DEVELOPER = "F4TAL";

    private static final String REQUIRED_VALUE = "F4TAL";
    private static final String BASE_PACKAGE   = "com.acadscatchup";

    public static void main(String[] args) {
        verifyAll();
    }

    /**
     * Verify ALL classes in the project have DEVELOPER = "F4TAL".
     * Call this ONCE at startup before anything else runs.
     */
    public static void verifyAll() {
        System.out.println("[F4TAL Guard] ⚙ Developer integrity check starting...");

        List<Class<?>> classes = discoverClasses(BASE_PACKAGE);
        List<String> failures = new ArrayList<>();

        for (Class<?> clazz : classes) {
            try {
                Field field = clazz.getDeclaredField("DEVELOPER");
                field.setAccessible(true);

                // Must be static
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    failures.add(clazz.getName() + " — DEVELOPER field is not static");
                    continue;
                }

                Object value = field.get(null);
                if (!REQUIRED_VALUE.equals(value)) {
                    failures.add(clazz.getName() + " — DEVELOPER value is '" + value + "' (expected '" + REQUIRED_VALUE + "')");
                }
            } catch (NoSuchFieldException e) {
                failures.add(clazz.getName() + " — MISSING 'public static final String DEVELOPER' field");
            } catch (Exception e) {
                failures.add(clazz.getName() + " — ERROR accessing DEVELOPER field: " + e.getMessage());
            }
        }

        if (!failures.isEmpty()) {
            System.err.println("╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║  FATAL: Developer integrity check FAILED                    ║");
            System.err.println("║  Unauthorized modification detected!                        ║");
            System.err.println("╠══════════════════════════════════════════════════════════════╣");
            for (String fail : failures) {
                System.err.println("║  ✗ " + fail);
            }
            System.err.println("╠══════════════════════════════════════════════════════════════╣");
            System.err.println("║  All Java classes MUST contain:                             ║");
            System.err.println("║  public static final String DEVELOPER = \"F4TAL\";            ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            throw new RuntimeException(
                    "[F4TAL Guard] FATAL — Developer integrity check FAILED. " +
                    failures.size() + " class(es) have unauthorized modifications. " +
                    "Application cannot start."
            );
        }

        System.out.println("[F4TAL Guard] ✔ All " + classes.size() + " classes verified — DEVELOPER = \"F4TAL\" ✓");
    }

    /**
     * Discover all .class files under the given package from the classpath.
     */
    private static List<Class<?>> discoverClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    // Running from filesystem (IDE / exploded classes)
                    File directory = new File(resource.toURI());
                    scanDirectory(directory, packageName, classes);
                } else if ("jar".equals(protocol)) {
                    // Running from JAR
                    String jarPath = resource.getPath();
                    // Format: file:/path/to/jar.jar!/com/acadscatchup
                    jarPath = jarPath.substring(5, jarPath.indexOf("!"));
                    scanJar(new java.util.jar.JarFile(jarPath), packageName, classes);
                }
            }
        } catch (Exception e) {
            System.err.println("[F4TAL Guard] Warning: Could not fully scan classpath — " + e.getMessage());
        }

        return classes;
    }

    /**
     * Recursively scan a directory for .class files.
     */
    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                loadAndAdd(className, classes);
            }
        }
    }

    /**
     * Scan a JAR file for classes in the given package.
     */
    private static void scanJar(java.util.jar.JarFile jarFile, String packageName, List<Class<?>> classes) {
        String prefix = packageName.replace('.', '/') + "/";
        Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            java.util.jar.JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.startsWith(prefix) && name.endsWith(".class") && !name.contains("$")) {
                String className = name.replace('/', '.').replace(".class", "");
                loadAndAdd(className, classes);
            }
        }

        try { jarFile.close(); } catch (IOException ignored) {}
    }

    /**
     * Load a class and add it to the list. Skip anonymous/inner classes.
     */
    private static void loadAndAdd(String className, List<Class<?>> classes) {
        // Skip inner/anonymous classes (e.g., Foo$1, Foo$Bar)
        if (className.contains("$")) return;
        // Skip this guard class itself to prevent circular validation issues
        // (we already know it's valid since we're running!)

        try {
            Class<?> clazz = Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());
            classes.add(clazz);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Skip classes that can't be loaded (e.g., test classes, optional deps)
            System.err.println("[F4TAL Guard] Skipped (not loadable): " + className);
        }
    }
}
