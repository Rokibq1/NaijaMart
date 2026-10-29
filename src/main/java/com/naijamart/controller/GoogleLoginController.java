
package com.naijamart.controller;

import com.naijamart.security.GoogleLoginSuccessHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleLoginController {

    private final GoogleLoginSuccessHandler successHandler;

    public GoogleLoginController(
            GoogleLoginSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(
            @RequestBody Map<String, String> requestBody) {

        String code = requestBody.get("code");

        String token = successHandler.consumeLoginCode(code);

        if (token == null) {
            Map<String, String> error = new HashMap<>();
            error.put(
                    "message",
                    "Google login code is invalid or expired. Please try again."
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(error);
        }

        Map<String, String> result = new HashMap<>();
        result.put("message", "Google login successful");
        result.put("token", token);

        return ResponseEntity.ok(result);
    }
}
