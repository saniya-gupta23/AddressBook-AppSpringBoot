package com.BridgeLabz.AddressBook.App.Service;

import com.BridgeLabz.AddressBook.App.DTO.LoginRequestDTO;
import com.BridgeLabz.AddressBook.App.DTO.LoginResponseDTO;
import com.BridgeLabz.AddressBook.App.DTO.UserDTO;
import com.BridgeLabz.AddressBook.App.Entity.User;
import com.BridgeLabz.AddressBook.App.Repository.UserRepository;
import com.BridgeLabz.AddressBook.App.Security.JwtUtil;
import jakarta.mail.MessagingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtils;

    @Mock
    private EmailService emailService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @Mock
    private ValueOperations<String, String> valueOperations; // ✅ Mock ValueOperations

    @Before
    public void setUp() {
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        MockitoAnnotations.openMocks(this);

        // ✅ Ensure redisTemplate.opsForValue() returns a mock ValueOperations object
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testRegisterUser_Success() {
        UserDTO userDTO = new UserDTO("testUser", "test@example.com", "password123");

        when(userRepository.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(userDTO.getPassword())).thenReturn("encodedPassword");

        String result = authService.registerUser(userDTO);

        assertEquals("User registered successfully!", result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegisterUser_EmailAlreadyExists() {
        UserDTO userDTO = new UserDTO("testUser", "test@example.com", "password123");

        when(userRepository.existsByEmail(userDTO.getEmail())).thenReturn(true);

        String result = authService.registerUser(userDTO);

        assertEquals("Error: Email already registered!", result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLoginUser_Success() throws MessagingException {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("test@example.com", "password123");

        when(userRepository.findByEmail(loginRequestDTO.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtUtils.generateToken(testUser.getEmail())).thenReturn("mocked-jwt-token");

        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        LoginResponseDTO response = authService.loginUser(loginRequestDTO);

        assertEquals("Login successful! Check your email for the token.", response.getMessage());
        assertEquals("mocked-jwt-token", response.getToken());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoginUser_InvalidPassword() throws MessagingException {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("test@example.com", "wrongPassword");

        when(userRepository.findByEmail(loginRequestDTO.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequestDTO.getPassword(), testUser.getPassword())).thenReturn(false);

        authService.loginUser(loginRequestDTO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoginUser_UserNotFound() throws MessagingException {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("notfound@example.com", "password123");

        when(userRepository.findByEmail(loginRequestDTO.getEmail())).thenReturn(Optional.empty());

        authService.loginUser(loginRequestDTO);
    }

    @Test
    public void testLoginWithToken_Success() throws MessagingException {
        String token = "mocked-jwt-token";

        when(jwtUtils.getEmailFromToken(token)).thenReturn("test@example.com");
        when(redisTemplate.opsForValue().get("JWT_TOKEN:test@example.com")).thenReturn(token);
        when(jwtUtils.isTokenValid("test@example.com", token)).thenReturn(true);

        boolean result = authService.loginWithToken(token);

        assertTrue(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoginWithToken_InvalidToken() throws MessagingException {
        String token = "invalid-token";

        when(jwtUtils.getEmailFromToken(token)).thenReturn("test@example.com");
        when(redisTemplate.opsForValue().get("JWT_TOKEN:test@example.com")).thenReturn(null);

        authService.loginWithToken(token);
    }

    @Test
    public void testForgotPassword_Success() throws MessagingException {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        boolean result = authService.forgotPassword(testUser.getEmail(), "newPassword");

        assertTrue(result);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    public void testForgotPassword_UserNotFound() throws MessagingException {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        boolean result = authService.forgotPassword("notfound@example.com", "newPassword");

        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testResetPassword_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        boolean result = authService.resetPassword(testUser.getEmail(), "oldPassword", "newPassword");

        assertTrue(result);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResetPassword_IncorrectOldPassword() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", testUser.getPassword())).thenReturn(false);

        authService.resetPassword(testUser.getEmail(), "wrongOldPassword", "newPassword");
    }

    @Test
    public void testResetPassword_UserNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        boolean result = authService.resetPassword("notfound@example.com", "oldPassword", "newPassword");

        assertFalse(result);
    }
}
