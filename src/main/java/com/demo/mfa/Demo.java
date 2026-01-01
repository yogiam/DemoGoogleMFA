package com.demo.mfa;

import com.demo.mfa.core.TotpAuthenticator;
import com.demo.mfa.model.User;
import com.demo.mfa.storage.JsonUserStorage;

/**
 * Non-interactive demo showing the MFA flow.
 */
public class Demo {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║              MFA Demo - Non-Interactive Mode                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        TotpAuthenticator auth = new TotpAuthenticator("DemoGoogleMFA");
        JsonUserStorage storage = new JsonUserStorage("users.json");

        String testEmail = "demo@example.com";
        String testPassword = "SecurePass123!";

        // ========== PART 1: USER ONBOARDING ==========
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  PART 1: USER ONBOARDING (First-time MFA Setup)");
        System.out.println("=".repeat(65));

        System.out.println("\n[Step 1] Generate TOTP secret for new user...");
        String secret = auth.generateSecretKey();
        System.out.println("  Secret Key: " + secret);

        System.out.println("\n[Step 2] Generate QR code for authenticator app...");
        String otpAuthUri = auth.generateOtpAuthUri(secret, testEmail);
        System.out.println("  OTP Auth URI: " + otpAuthUri);

        System.out.println("\n[Step 3] Display QR code (scan with Google Authenticator/Authy):\n");
        System.out.println(auth.generateQrCodeAscii(otpAuthUri));

        // Save QR code as image
        String qrFile = "demo-qrcode.png";
        auth.generateQrCodeImage(otpAuthUri, qrFile);
        System.out.println("  QR code also saved to: " + qrFile);

        System.out.println("\n[Step 4] Verify setup - current valid code: " +
                          String.format("%06d", auth.getCurrentCode(secret)));

        // Save user
        User user = new User(testEmail, testPassword, secret, true);
        storage.save(user);
        System.out.println("\n[Step 5] User saved to storage");
        System.out.println("  Email: " + testEmail);
        System.out.println("  MFA Enabled: true");

        // ========== PART 2: USER AUTHENTICATION ==========
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  PART 2: USER AUTHENTICATION (Login with MFA)");
        System.out.println("=".repeat(65));

        System.out.println("\n[Step 1] User enters credentials...");
        System.out.println("  Email: " + testEmail);
        System.out.println("  Password: ********");

        System.out.println("\n[Step 2] Verify password...");
        User loadedUser = storage.findByEmail(testEmail).orElseThrow();
        boolean passwordValid = loadedUser.verifyPassword(testPassword);
        System.out.println("  Password valid: " + passwordValid);

        System.out.println("\n[Step 3] MFA Challenge - get current TOTP code...");
        int currentCode = auth.getCurrentCode(loadedUser.getTotpSecret());
        System.out.println("  Current valid code: " + String.format("%06d", currentCode));

        System.out.println("\n[Step 4] Validate TOTP code...");
        boolean codeValid = auth.validateCode(loadedUser.getTotpSecret(), currentCode);
        System.out.println("  Code valid: " + codeValid);

        System.out.println("\n[Step 5] Authentication result...");
        if (passwordValid && codeValid) {
            System.out.println("  ✓ LOGIN SUCCESSFUL!");
        } else {
            System.out.println("  ✗ LOGIN FAILED!");
        }

        // ========== PART 3: TESTING INVALID CODE ==========
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  PART 3: TESTING INVALID CODE");
        System.out.println("=".repeat(65));

        int wrongCode = 123456;
        System.out.println("\n[Test] Attempting login with wrong code: " + wrongCode);
        boolean wrongCodeValid = auth.validateCode(loadedUser.getTotpSecret(), wrongCode);
        System.out.println("  Code valid: " + wrongCodeValid);
        System.out.println("  Result: " + (wrongCodeValid ? "✓ Access granted" : "✗ Access denied"));

        // ========== SUMMARY ==========
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  DEMO COMPLETE");
        System.out.println("=".repeat(65));
        System.out.println("\nKey takeaways:");
        System.out.println("  • TotpAuthenticator is storage-agnostic (you handle persistence)");
        System.out.println("  • QR codes work with any TOTP authenticator app");
        System.out.println("  • Codes are time-based and change every 30 seconds");
        System.out.println("  • The same code cannot be reused (replay protection)");
        System.out.println("\nFiles created:");
        System.out.println("  • users.json - User data storage");
        System.out.println("  • demo-qrcode.png - Scannable QR code");
        System.out.println();
    }
}
