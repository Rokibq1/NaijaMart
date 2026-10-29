
package com.naijamart.service;

import com.naijamart.dto.LoginRequest;
import com.naijamart.entity.User;
import com.naijamart.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    // Public registration must always create a CUSTOMER.
    public User createUser(User user) {

        user.setRole(User.Role.CUSTOMER);

        String encryptedPassword =
                passwordEncoder.encode(user.getPassword());

        user.setPassword(encryptedPassword);

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> login(LoginRequest loginRequest) {

        Optional<User> userOptional =
                userRepository.findByEmail(loginRequest.getEmail());

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        boolean passwordMatches =
                passwordEncoder.matches(
                        loginRequest.getPassword(),
                        user.getPassword()
                );

        if (passwordMatches) {
            return Optional.of(user);
        }

        return Optional.empty();
    }
}
