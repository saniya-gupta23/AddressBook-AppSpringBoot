package com.BridgeLabz.AddressBook.App.Controller;

import com.BridgeLabz.AddressBook.App.DTO.ForgotPasswordDTO;
import com.BridgeLabz.AddressBook.App.DTO.LoginRequestDTO;
import com.BridgeLabz.AddressBook.App.DTO.LoginResponseDTO;
import com.BridgeLabz.AddressBook.App.DTO.UserDTO;
import com.BridgeLabz.AddressBook.App.Service.AuthService;
import com.BridgeLabz.AddressBook.App.Service.EmailService;
import com.BridgeLabz.AddressBook.App.Service.RabbitMQProducer;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j  // ✅ Lombok annotation for SLF4J logging
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    @Autowired
    private RabbitMQProducer rabbitMQProducer;

    @PostMapping("/send-message")
    public ResponseEntity<String> sendMessage(@RequestBody String message) {
        rabbitMQProducer.sendMessage(message);
        return ResponseEntity.ok("Message sent to RabbitMQ: " + message);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO userDTO) {
        log.info("Registering user: {}", userDTO.getUsername());  // ✅ Logging username

        String response = authService.registerUser(userDTO);
        emailService.sendRegistrationEmail(userDTO.getEmail());

        log.info("User registered successfully: {}", userDTO.getEmail());  // ✅ Log success
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequestDTO) throws MessagingException {
        log.info("Login request received for email: {}", loginRequestDTO.getEmail());

        LoginResponseDTO response = authService.loginUser(loginRequestDTO);

        log.debug("Generated JWT Token: {}", response.getToken());  // ✅ Log token for debugging

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-with-token")
    public ResponseEntity<?> loginWithToken(@RequestHeader("Authorization") String token) {
        try {
            String parsedToken = token.replace("Bearer ", "");
            log.info("Token-based login attempt.");

            if (authService.loginWithToken(parsedToken)) {
                log.info("Login successful with token.");
                return ResponseEntity.ok("Login successful with token.");
            }

            log.warn("Unauthorized token login attempt.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized.");
        } catch (IllegalArgumentException | MessagingException e) {
            log.error("Token login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PutMapping("/forgotPassword")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordDTO forgotPasswordDTO) throws MessagingException {
        log.info("Processing forgot password request for email: {}", forgotPasswordDTO.getEmail());

        boolean isUpdated = authService.forgotPassword(forgotPasswordDTO.getEmail(), forgotPasswordDTO.getNewPassword());
        if (!isUpdated) {
            log.warn("Failed password reset attempt for email: {}", forgotPasswordDTO.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sorry! We cannot find the user email: " + forgotPasswordDTO.getEmail());
        }

        log.info("Password changed successfully for email: {}", forgotPasswordDTO.getEmail());
        return ResponseEntity.ok("Password has been changed successfully!");
    }

    @PutMapping("/resetPassword/{email}")
    public ResponseEntity<String> resetPassword(@PathVariable String email, @RequestBody Map<String, String> request) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        log.info("Processing password reset request for email: {}", email);

        try {
            boolean isUpdated = authService.resetPassword(email, oldPassword, newPassword);
            if (!isUpdated) {
                log.warn("Reset password failed: User email not found {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sorry! We cannot find the user email: " + email);
            }

            log.info("Password reset successfully for email: {}", email);
            return ResponseEntity.ok("Password has been reset successfully!");
        } catch (IllegalArgumentException e) {
            log.error("Password reset error for email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
