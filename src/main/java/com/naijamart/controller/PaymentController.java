
package com.naijamart.controller;

import com.naijamart.service.PaymentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializePayment(
            @RequestBody Map<String, Long> request) {

        Long orderId = request.get("orderId");

        if (orderId == null || orderId <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "A valid orderId is required.");
        }

        return ResponseEntity.ok(
                paymentService.initializePayment(orderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @RequestBody Map<String, Object> request) {

        Object orderIdValue = request.get("orderId");
        Object referenceValue = request.get("reference");

        if (!(orderIdValue instanceof Number)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "A valid orderId is required.");
        }

        if (referenceValue == null
                || referenceValue.toString().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "A payment reference is required.");
        }

        Long orderId = ((Number) orderIdValue).longValue();

        return ResponseEntity.ok(
                paymentService.verifyPayment(
                        orderId,
                        referenceValue.toString()));
    }
}
