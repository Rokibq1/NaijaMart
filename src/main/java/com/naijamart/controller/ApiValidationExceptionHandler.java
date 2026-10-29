
package com.naijamart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        List<Map<String, String>> errors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> {
                            Map<String, String> detail =
                                    new LinkedHashMap<>();

                            detail.put("field", error.getField());
                            detail.put(
                                    "message",
                                    error.getDefaultMessage() != null
                                            ? error.getDefaultMessage()
                                            : "Invalid value");

                            return detail;
                        })
                        .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Checkout validation failed.");
        response.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
