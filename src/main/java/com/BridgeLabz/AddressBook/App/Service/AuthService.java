package com.BridgeLabz.AddressBook.App.Service;

import com.BridgeLabz.AddressBook.App.DTO.UserDTO;
import com.BridgeLabz.AddressBook.App.Entity.User;
import com.BridgeLabz.AddressBook.App.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Use PasswordEncoder interface

    public String registerUser(UserDTO userDTO) {
        // Check if email is already registered
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            return "Error: Email already registered!";
        }

        // Create new User object and hash password
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword())); // Hash password

        // Save user to database
        userRepository.save(user);

        return "User registered successfully!";
    }
}
