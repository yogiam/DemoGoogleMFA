# DemoGoogleMFA - MFA Integration Demo Application

## Overview

A standalone Java 17 CLI application demonstrating how to integrate TOTP-based Multi-Factor Authentication (MFA) into any application. Uses the `com.warrenstrange:googleauth` library for TOTP generation and validation.

## Features

- **User Registration with MFA Onboarding**: Generate TOTP secrets, display QR codes (ASCII + PNG), verify setup
- **User Login with MFA Challenge**: Password verification followed by TOTP code validation
- **Reusable MFA Component**: Storage-agnostic `TotpAuthenticator` class for easy integration into other projects
- **JSON-based User Storage**: Simple file-based persistence for demo purposes

## Project Structure

```
DemoGoogleMFA/
├── build.gradle                                    # Gradle build configuration
├── settings.gradle                                 # Gradle settings
├── claude.md                                       # This documentation file
├── users.json                                      # User data (created at runtime)
├── qrcode.png                                      # QR code image (created during registration)
└── src/main/java/com/demo/mfa/
    ├── core/
    │   └── TotpAuthenticator.java                  # Reusable MFA class (storage-agnostic)
    ├── model/
    │   └── User.java                               # User entity (email, password, TOTP secret)
    ├── storage/
    │   └── JsonUserStorage.java                    # JSON file-based user persistence
    └── MfaDemoApp.java                             # CLI application entry point
```

## Requirements

- Java 17 or higher
- Gradle (or use the Gradle wrapper)

## Building and Running

### Build the project
```bash
./gradlew build
```

### Run the application
```bash
./gradlew run --console=plain
```

### Create a distributable JAR
```bash
./gradlew jar
```

## Usage

### Main Menu
```
=== Main Menu ===
1. Register new user (with MFA onboarding)
2. Login (with MFA challenge)
3. Exit
```

### Registration Flow
1. Enter email address
2. Enter and confirm password
3. Scan the QR code with an authenticator app (Google Authenticator, Authy, etc.)
4. Enter the 6-digit code from the app to verify setup
5. Registration complete with MFA enabled

### Login Flow
1. Enter email address
2. Enter password
3. Enter the 6-digit code from authenticator app
4. Login successful

## Reusing TotpAuthenticator in Other Projects

The `TotpAuthenticator` class is designed to be storage-agnostic and easily portable. It handles all TOTP operations without any dependencies on how/where secrets are stored.

### Integration Steps

1. **Add dependencies** to your project:
   ```groovy
   implementation 'com.warrenstrange:googleauth:1.5.0'
   implementation 'com.google.zxing:core:3.5.2'
   implementation 'com.google.zxing:javase:3.5.2'
   ```

2. **Copy `TotpAuthenticator.java`** to your project

3. **Use in your application**:

### Code Examples

#### User Onboarding (Enrollment)
```java
TotpAuthenticator auth = new TotpAuthenticator("YourAppName");

// Generate a new secret for the user
String secret = auth.generateSecretKey();

// Generate QR code URI
String otpAuthUri = auth.generateOtpAuthUri(secret, "user@example.com");

// Display QR code (choose one or both)
System.out.println(auth.generateQrCodeAscii(otpAuthUri));  // ASCII art
auth.generateQrCodeImage(otpAuthUri, "qrcode.png");        // PNG file

// Store the secret securely for this user
userRepository.saveSecret(userId, secret);
```

#### User Authentication (Challenge)
```java
TotpAuthenticator auth = new TotpAuthenticator("YourAppName");

// Retrieve the user's stored secret
String secret = userRepository.getSecret(userId);

// Get the code entered by the user
int userCode = Integer.parseInt(userInput);

// Validate the code
boolean isValid = auth.validateCode(secret, userCode);

if (isValid) {
    // Grant access
} else {
    // Deny access
}
```

#### Testing/Demo: Get Current Code
```java
TotpAuthenticator auth = new TotpAuthenticator("YourAppName");
String secret = "JBSWY3DPEHPK3PXP";  // Example secret

// Get the current valid code (useful for testing)
int currentCode = auth.getCurrentCode(secret);
System.out.println("Current code: " + currentCode);
```

## API Reference

### TotpAuthenticator

| Method | Description |
|--------|-------------|
| `TotpAuthenticator(String issuer)` | Create authenticator with issuer name |
| `String generateSecretKey()` | Generate a new Base32-encoded secret |
| `String generateOtpAuthUri(String secret, String accountName)` | Create otpauth:// URI for QR codes |
| `String generateQrCodeAscii(String otpAuthUri)` | Generate QR code as ASCII art |
| `void generateQrCodeImage(String otpAuthUri, String filePath)` | Save QR code as PNG file |
| `boolean validateCode(String secret, int code)` | Validate a TOTP code |
| `boolean validateCode(String secret, String codeStr)` | Validate a TOTP code (String version) |
| `int getCurrentCode(String secret)` | Get current valid code (for testing) |

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `com.warrenstrange:googleauth` | 1.5.0 | TOTP generation and validation |
| `com.google.zxing:core` | 3.5.2 | QR code generation |
| `com.google.zxing:javase` | 3.5.2 | QR code image output |
| `com.google.code.gson:gson` | 2.10.1 | JSON serialization for user storage |

## Security Notes

- **Password Hashing**: This demo uses SHA-256 for simplicity. For production, use bcrypt, scrypt, or Argon2.
- **Secret Storage**: TOTP secrets should be stored encrypted at rest in production.
- **HTTPS**: Always use HTTPS in production to protect credentials in transit.
- **Rate Limiting**: Implement rate limiting on login attempts to prevent brute force attacks.

## License

This is a demonstration project for educational purposes.
