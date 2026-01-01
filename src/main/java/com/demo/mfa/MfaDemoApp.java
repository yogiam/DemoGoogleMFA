package com.demo.mfa;

import com.demo.mfa.core.TotpAuthenticator;
import com.demo.mfa.model.User;
import com.demo.mfa.storage.JsonUserStorage;

import java.io.Console;
import java.util.Optional;
import java.util.Scanner;

/**
 * MFA Demo Application - Command Line Interface
 *
 * Demonstrates:
 * 1. User registration with MFA onboarding
 * 2. User login with MFA challenge
 */
public class MfaDemoApp {

    private static final String ISSUER = "DemoGoogleMFA";
    private static final String USERS_FILE = "users.json";
    private static final String QR_CODE_FILE = "qrcode.png";

    private final TotpAuthenticator authenticator;
    private final JsonUserStorage storage;
    private final Scanner scanner;

    public MfaDemoApp() {
        this.authenticator = new TotpAuthenticator(ISSUER);
        this.storage = new JsonUserStorage(USERS_FILE);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        MfaDemoApp app = new MfaDemoApp();
        app.run();
    }

    /**
     * Main application loop.
     */
    public void run() {
        printBanner();

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> registerUser();
                case "2" -> loginUser();
                case "3" -> {
                    System.out.println("\nGoodbye!");
                    return;
                }
                default -> System.out.println("\nInvalid option. Please try again.");
            }
        }
    }

    /**
     * Prints the application banner.
     */
    private void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║       MFA Demo Application                ║");
        System.out.println("║   TOTP Authentication with Google Auth    ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Prints the main menu.
     */
    private void printMenu() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("1. Register new user (with MFA onboarding)");
        System.out.println("2. Login (with MFA challenge)");
        System.out.println("3. Exit");
        System.out.print("\nSelect option: ");
    }

    /**
     * Handles user registration with MFA onboarding.
     */
    private void registerUser() {
        System.out.println("\n=== User Registration ===\n");

        // Get email
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        if (email.isEmpty()) {
            System.out.println("\nError: Email cannot be empty.");
            return;
        }

        if (storage.exists(email)) {
            System.out.println("\nError: A user with this email already exists.");
            return;
        }

        // Get password
        String password = readPassword("Enter password: ");
        if (password.isEmpty()) {
            System.out.println("\nError: Password cannot be empty.");
            return;
        }

        String confirmPassword = readPassword("Confirm password: ");
        if (!password.equals(confirmPassword)) {
            System.out.println("\nError: Passwords do not match.");
            return;
        }

        // Generate TOTP secret
        String secret = authenticator.generateSecretKey();
        String otpAuthUri = authenticator.generateOtpAuthUri(secret, email);

        System.out.println("\n=== MFA Setup ===\n");
        System.out.println("Scan this QR code with your authenticator app");
        System.out.println("(Google Authenticator, Authy, etc.):\n");

        // Display QR code as ASCII
        System.out.println(authenticator.generateQrCodeAscii(otpAuthUri));

        // Save QR code as image
        try {
            authenticator.generateQrCodeImage(otpAuthUri, QR_CODE_FILE);
            System.out.println("QR code also saved to: " + QR_CODE_FILE);
        } catch (Exception e) {
            System.out.println("Note: Could not save QR code image: " + e.getMessage());
        }

        // Display manual entry option
        System.out.println("\nOr enter this key manually:");
        System.out.println("Secret: " + secret);
        System.out.println("Issuer: " + ISSUER);
        System.out.println("Account: " + email);

        // Verify setup by asking for a code
        System.out.println("\n=== Verify Setup ===");
        System.out.print("\nEnter the 6-digit code from your authenticator app: ");
        String codeStr = scanner.nextLine().trim();

        if (!authenticator.validateCode(secret, codeStr)) {
            System.out.println("\nError: Invalid code. Registration cancelled.");
            System.out.println("Please try again and make sure your authenticator app is set up correctly.");
            return;
        }

        // Save user
        User user = new User(email, password, secret, true);
        storage.save(user);

        System.out.println("\n✓ Registration successful!");
        System.out.println("  MFA has been enabled for your account.");
        System.out.println("  You can now login with your credentials and authenticator code.");
    }

    /**
     * Handles user login with MFA challenge.
     */
    private void loginUser() {
        System.out.println("\n=== User Login ===\n");

        // Get email
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        if (email.isEmpty()) {
            System.out.println("\nError: Email cannot be empty.");
            return;
        }

        // Find user
        Optional<User> userOpt = storage.findByEmail(email);
        if (userOpt.isEmpty()) {
            System.out.println("\nError: User not found.");
            return;
        }

        User user = userOpt.get();

        // Get password
        String password = readPassword("Enter password: ");

        // Verify password
        if (!user.verifyPassword(password)) {
            System.out.println("\nError: Invalid password.");
            return;
        }

        // MFA challenge
        if (user.isMfaEnabled()) {
            System.out.println("\n=== MFA Challenge ===");
            System.out.print("\nEnter the 6-digit code from your authenticator app: ");
            String codeStr = scanner.nextLine().trim();

            if (!authenticator.validateCode(user.getTotpSecret(), codeStr)) {
                System.out.println("\nError: Invalid authentication code.");
                return;
            }
        }

        System.out.println("\n✓ Login successful!");
        System.out.println("  Welcome, " + email + "!");
    }

    /**
     * Reads a password from the console.
     * Falls back to regular input if console is not available.
     *
     * @param prompt The prompt to display
     * @return The entered password
     */
    private String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt);
            return new String(passwordChars);
        } else {
            // Fallback for IDEs and environments without console
            System.out.print(prompt);
            return scanner.nextLine();
        }
    }
}
