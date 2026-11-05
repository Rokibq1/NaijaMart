
package com.naijamart.service;

import com.naijamart.dto.LoginRequest;
import com.naijamart.entity.User;
import com.naijamart.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Public registration always creates a CUSTOMER.
    public User createUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User information is required");
        }

        user.setEmail(normalizeEmail(user.getEmail()));
        user.setRole(User.Role.CUSTOMER);

        String encryptedPassword =
                passwordEncoder.encode(user.getPassword());

        user.setPassword(encryptedPassword);

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    public Optional<User> login(LoginRequest loginRequest) {

        if (loginRequest == null
                || loginRequest.getEmail() == null
                || loginRequest.getPassword() == null) {
            return Optional.empty();
        }

        Optional<User> userOptional =
                userRepository.findByEmail(
                        normalizeEmail(loginRequest.getEmail())
                );

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        boolean passwordMatches =
                passwordEncoder.matches(
                        loginRequest.getPassword(),
                        user.getPassword()
                );

        return passwordMatches
                ? Optional.of(user)
                : Optional.empty();
    }

    private String normalizeEmail(String email) {

        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
