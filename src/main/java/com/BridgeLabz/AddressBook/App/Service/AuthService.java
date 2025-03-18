package com.BridgeLabz.AddressBook.App.Service;

import com.BridgeLabz.AddressBook.App.DTO.LoginRequestDTO;
import com.BridgeLabz.AddressBook.App.DTO.LoginResponseDTO;
import com.BridgeLabz.AddressBook.App.DTO.UserDTO;
import com.BridgeLabz.AddressBook.App.Entity.User;
import com.BridgeLabz.AddressBook.App.Repository.UserRepository;
import com.BridgeLabz.AddressBook.App.Security.JwtUtil;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;  // ✅ Autowire RedisTemplate

    private static final String TOKEN_PREFIX = "JWT_TOKEN:";  // ✅ Key prefix for Redis storage

    // Register User
    public String registerUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            return "Error: Email already registered!";
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        userRepository.save(user);
        return "User registered successfully!";
    }

    // Login User (Email & Password)
    public LoginResponseDTO loginUser(LoginRequestDTO loginRequestDTO) throws MessagingException {
        User user = userRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password!");
        }

        // Generate and store a new token
        String token = jwtUtils.generateToken(user.getEmail());

        // Store token in Redis with a 1-hour expiry
        redisTemplate.opsForValue().set(TOKEN_PREFIX + user.getEmail(), token, 1, TimeUnit.HOURS);

        // Send JWT token in email
        sendTokenEmail(user.getEmail(), token);

        return new LoginResponseDTO("Login successful! Check your email for the token.", token);
    }

    public boolean loginWithToken(String token) throws MessagingException {
        String email = jwtUtils.getEmailFromToken(token);

        if (email != null) {
            //  Retrieve token from Redis
            String storedToken = redisTemplate.opsForValue().get(TOKEN_PREFIX + email);

            if (storedToken != null && storedToken.equals(token) && jwtUtils.isTokenValid(email, token)) {
                //  Send login success email
                sendLoginSuccessEmail(email);
                return true;
            }
        }

        throw new IllegalArgumentException("Invalid or expired token!");
    }

    // ✅ Send JWT Token Email
    private void sendTokenEmail(String email, String token) throws MessagingException {
        String subject = "Your JWT Token for Login";
        String message = "<p style='font-family: Arial, sans-serif; color: #333;'>Hello,</p>" +
                "<p style='font-size: 14px; color: #555;'>Your login was successful. Here is your JWT token:</p>" +
                "<p style='font-size: 16px; color: blue; font-weight: bold;'>" + token + "</p>" +
                "<p style='font-size: 14px; color: #555;'>Use this token for authentication.</p>" +
                "<p style='font-size: 14px; color: #333;'>Regards,<br><strong>AddressBook App Team</strong></p>";

        emailService.sendEmail(email, subject, message);
    }

    // ✅ Send Login Success Email
    private void sendLoginSuccessEmail(String email) throws MessagingException {
        String subject = "Login Success Notification";
        String message = "<p style='font-family: Arial, sans-serif; color: #333;'>Hello,</p>" +
                "<p style='font-family: Arial, sans-serif; color: #333;'>You have successfully logged in using your token.</p>" +
                "<p style='font-family: Arial, sans-serif; color: red; font-weight: bold;'>If this wasn't you, please reset your password immediately.</p>" +
                "<p style='font-family: Arial, sans-serif; font-weight: bold;'>Regards,<br>AddressBook App Team</p>";

        emailService.sendEmail(email, subject, message);
    }

    // Forgot Password
    public boolean forgotPassword(String email, String newPassword) throws MessagingException {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            String subject = "Password Changed Successfully";
            String message = "Hello " + user.getUsername() + ",\n\nYour password has been changed.\n\nIf this wasn't you, contact support immediately.";
            emailService.sendEmail(user.getEmail(), subject, message);
            return true;
        }
        return false;
    }

    // Reset Password
    public boolean resetPassword(String email, String oldPassword, String newPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new IllegalArgumentException("Old password is incorrect!");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
