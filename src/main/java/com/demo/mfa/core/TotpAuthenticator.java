package com.demo.mfa.core;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A storage-agnostic TOTP (Time-based One-Time Password) authenticator.
 *
 * This class provides all the core functionality needed for MFA:
 * - Secret key generation for user enrollment
 * - QR code generation (ASCII and PNG)
 * - TOTP code validation
 *
 * The class does NOT handle storage. Callers are responsible for
 * persisting and retrieving user secrets.
 *
 * Usage example:
 * <pre>
 * TotpAuthenticator auth = new TotpAuthenticator("MyApp");
 *
 * // Onboarding
 * String secret = auth.generateSecretKey();
 * String uri = auth.generateOtpAuthUri(secret, "user@example.com");
 * System.out.println(auth.generateQrCodeAscii(uri));
 * auth.generateQrCodeImage(uri, "qrcode.png");
 * // Store 'secret' for the user
 *
 * // Authentication
 * String storedSecret = // retrieve from storage
 * boolean valid = auth.validateCode(storedSecret, userEnteredCode);
 * </pre>
 */
public class TotpAuthenticator {

    private final GoogleAuthenticator gAuth;
    private final String issuer;

    private static final int QR_CODE_SIZE = 300;
    private static final int ASCII_QR_SIZE = 25;

    /**
     * Creates a new TotpAuthenticator with the specified issuer name.
     *
     * @param issuer The name that will appear in authenticator apps (e.g., "MyApp")
     */
    public TotpAuthenticator(String issuer) {
        this.gAuth = new GoogleAuthenticator();
        this.issuer = issuer;
    }

    /**
     * Generates a new secret key for user enrollment.
     *
     * @return Base32-encoded secret key
     */
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Generates an otpauth:// URI for QR code generation.
     *
     * @param secret      The Base32-encoded secret key
     * @param accountName The user's account identifier (typically email)
     * @return The otpauth:// URI
     */
    public String generateOtpAuthUri(String secret, String accountName) {
        return generateOtpAuthUri(secret, accountName, this.issuer);
    }

    /**
     * Generates an otpauth:// URI for QR code generation with a custom issuer.
     *
     * @param secret      The Base32-encoded secret key
     * @param accountName The user's account identifier (typically email)
     * @param issuer      The issuer name to use
     * @return The otpauth:// URI
     */
    public String generateOtpAuthUri(String secret, String accountName, String issuer) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
            encodedIssuer,
            encodedAccount,
            secret,
            encodedIssuer
        );
    }

    /**
     * Generates a QR code as ASCII art for terminal display.
     *
     * @param otpAuthUri The otpauth:// URI to encode
     * @return ASCII representation of the QR code
     */
    public String generateQrCodeAscii(String otpAuthUri) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(
                otpAuthUri,
                BarcodeFormat.QR_CODE,
                ASCII_QR_SIZE,
                ASCII_QR_SIZE,
                hints
            );

            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < bitMatrix.getHeight(); y++) {
                for (int x = 0; x < bitMatrix.getWidth(); x++) {
                    sb.append(bitMatrix.get(x, y) ? "\u2588\u2588" : "  ");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (WriterException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generates a QR code and saves it as a PNG image file.
     *
     * @param otpAuthUri The otpauth:// URI to encode
     * @param filePath   The path where the PNG file should be saved
     */
    public void generateQrCodeImage(String otpAuthUri, String filePath) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(
                otpAuthUri,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                hints
            );

            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", Path.of(filePath));
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    /**
     * Validates a TOTP code against a secret.
     *
     * @param secret The Base32-encoded secret key
     * @param code   The 6-digit TOTP code entered by the user
     * @return true if the code is valid, false otherwise
     */
    public boolean validateCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    /**
     * Validates a TOTP code against a secret (String version).
     *
     * @param secret   The Base32-encoded secret key
     * @param codeStr  The TOTP code as a string
     * @return true if the code is valid, false otherwise
     */
    public boolean validateCode(String secret, String codeStr) {
        try {
            int code = Integer.parseInt(codeStr.trim());
            return validateCode(secret, code);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Gets the current valid TOTP code for a secret.
     * Useful for testing and demonstration purposes.
     *
     * @param secret The Base32-encoded secret key
     * @return The current 6-digit TOTP code
     */
    public int getCurrentCode(String secret) {
        return gAuth.getTotpPassword(secret);
    }

    /**
     * Gets the issuer name configured for this authenticator.
     *
     * @return The issuer name
     */
    public String getIssuer() {
        return issuer;
    }
}
