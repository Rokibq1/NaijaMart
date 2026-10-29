
package com.naijamart.service;

import com.naijamart.entity.User;
import com.naijamart.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class GoogleAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User findOrCreateCustomer(
            String email,
            String firstName,
            String lastName) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Google did not provide an email address."
            );
        }

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        // Reuse an existing account without changing its password or role.
        var existingUser =
                userRepository.findByEmail(normalizedEmail);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User user = new User();

        user.setEmail(normalizedEmail);
        user.setFirstName(
                firstName == null || firstName.isBlank()
                        ? "NaijaMart"
                        : firstName.trim()
        );
        user.setLastName(
                lastName == null || lastName.isBlank()
                        ? "Customer"
                        : lastName.trim()
        );

        // The entity requires a password. Generate a random value and
        // store only its BCrypt hash; never use Google's access token
        // or client secret as the customer's password.
        user.setPassword(
                passwordEncoder.encode(UUID.randomUUID().toString())
        );

        user.setRole(User.Role.CUSTOMER);

        return userRepository.save(user);
    }
}
