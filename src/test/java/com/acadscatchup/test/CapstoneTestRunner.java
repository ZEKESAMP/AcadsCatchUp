package com.acadscatchup.test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * CapstoneTestRunner — Zero-Dependency Standalone SQA Test Automation Suite.
 * Executes all unit and integration test suites, records microsecond timings,
 * and prints an ANSI-colored test execution report for academic defense panels.
 *
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class CapstoneTestRunner {

    public static final String DEVELOPER = "F4TAL";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  ACADSCATCHUP — CAPSTONE SQA AUTOMATED TEST SUITE EXECUTION");
        System.out.println("  Architecture: Java 21 LTS | Test Framework: Standalone SQA Engine");
        System.out.println("  Developer / Lead Architect: Stevenson James G. Gastanes (F4TAL)");
        System.out.println("================================================================================\n");

        Class<?>[] testClasses = new Class<?>[]{
                com.acadscatchup.util.PasswordUtilTest.class,
                com.acadscatchup.util.DeveloperGuardTest.class,
                com.acadscatchup.util.EmailServiceTest.class,
                com.acadscatchup.util.CSVExporterTest.class,
                com.acadscatchup.model.ModelIntegrityTest.class
        };

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        List<String> failureDetails = new ArrayList<>();
        long totalStart = System.currentTimeMillis();

        for (Class<?> clazz : testClasses) {
            System.out.println("▶ Executing Test Suite: " + clazz.getSimpleName());
            Method[] methods = clazz.getDeclaredMethods();

            Object testInstance;
            try {
                testInstance = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("  [FATAL] Could not instantiate test class " + clazz.getName() + ": " + e.getMessage());
                failedTests++;
                continue;
            }

            for (Method method : methods) {
                if (method.getName().startsWith("test") && Modifier.isPublic(method.getModifiers())) {
                    totalTests++;
                    long start = System.nanoTime();
                    try {
                        method.invoke(testInstance);
                        long durationMs = (System.nanoTime() - start) / 1_000_000;
                        System.out.printf("  ✔ PASS: %-32s (%d ms)%n", method.getName(), durationMs);
                        passedTests++;
                    } catch (Throwable t) {
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        long durationMs = (System.nanoTime() - start) / 1_000_000;
                        System.out.printf("  ✗ FAIL: %-32s (%d ms) -> %s%n", method.getName(), durationMs, cause.getMessage());
                        failedTests++;
                        failureDetails.add(clazz.getSimpleName() + "." + method.getName() + ": " + cause.toString());
                    }
                }
            }
            System.out.println();
        }

        long totalDuration = System.currentTimeMillis() - totalStart;

        System.out.println("================================================================================");
        System.out.println("  FINAL SQA TEST RESULTS SUMMARY");
        System.out.println("================================================================================");
        System.out.printf("  Total Test Cases Executed : %d%n", totalTests);
        System.out.printf("  Test Cases Passed         : %d%n", passedTests);
        System.out.printf("  Test Cases Failed         : %d%n", failedTests);
        System.out.printf("  Overall Pass Rate         : %.1f%%%n", (totalTests > 0 ? ((double) passedTests / totalTests) * 100.0 : 0.0));
        System.out.printf("  Execution Wall Time       : %d ms%n", totalDuration);
        System.out.println("================================================================================");

        if (failedTests > 0) {
            System.err.println("\n[FAILURE SUMMARY]");
            for (String detail : failureDetails) {
                System.err.println(" - " + detail);
            }
            System.exit(1);
        } else {
            System.out.println("✔ ALL CAPSTONE SQA UNIT TESTS COMPLETED SUCCESSFULLY! (VERIFIED GRADE A+)");
            System.exit(0);
        }
    }
}
