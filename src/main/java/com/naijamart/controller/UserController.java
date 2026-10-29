
package com.naijamart.controller;

import com.naijamart.dto.LoginRequest;
import com.naijamart.entity.User;
import com.naijamart.security.JwtService;
import com.naijamart.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(
            UserService userService,
            JwtService jwtService) {

        this.userService = userService;
        this.jwtService = jwtService;
    }

    // Register a new customer
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody User user) {

        if (user.getEmail() == null
                || user.getEmail().isBlank()
                || user.getPassword() == null
                || user.getPassword().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body("Email and password are required");
        }

        if (userService.emailExists(user.getEmail())) {

            return ResponseEntity
                    .badRequest()
                    .body("Email already exists");
        }

        User savedUser = userService.createUser(user);

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Registration successful");
        response.put("id", savedUser.getId());
        response.put("firstName", savedUser.getFirstName());
        response.put("lastName", savedUser.getLastName());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole().name());

        return ResponseEntity.ok(response);
    }

    // Find a customer by email
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(
            @PathVariable String email) {

        return userService.findByEmail(email)
                .<ResponseEntity<?>>map(user -> {

                    Map<String, Object> response =
                            new HashMap<>();

                    response.put("id", user.getId());
                    response.put("firstName", user.getFirstName());
                    response.put("lastName", user.getLastName());
                    response.put("email", user.getEmail());
                    response.put("role", user.getRole().name());

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    // Log in and generate a JWT token
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest) {

        if (loginRequest.getEmail() == null
                || loginRequest.getEmail().isBlank()
                || loginRequest.getPassword() == null
                || loginRequest.getPassword().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body("Email and password are required");
        }

        var userOptional = userService.login(loginRequest);

        if (userOptional.isEmpty()) {

            return ResponseEntity
                    .status(401)
                    .body("Invalid email or password");
        }

        User user = userOptional.get();

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Login successful");
        response.put("token", token);
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        return ResponseEntity.ok(response);
    }
}
