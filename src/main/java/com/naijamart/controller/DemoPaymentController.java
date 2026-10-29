
package com.naijamart.controller;

import com.naijamart.order.PurchaseOrder;
import com.naijamart.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo-payments")
public class DemoPaymentController {

    private final OrderService orderService;

    public DemoPaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/success")
    public Map<String, Object> simulateSuccessfulPayment(
            @RequestBody Map<String, Object> request) {

        Object orderIdValue = request.get("orderId");

        if (!(orderIdValue instanceof Number)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid order ID is required.");
        }

        Long orderId = ((Number) orderIdValue).longValue();

        PurchaseOrder order =
                orderService.confirmSimulatedPayment(orderId);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put(
                "message",
                "Simulated payment successful. No real money was charged.");
        result.put("simulated", true);
        result.put("orderId", order.getId());
        result.put("trackingNumber", order.getTrackingNumber());
        result.put("paymentReference", order.getPaymentReference());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("orderStatus", order.getStatus());
        result.put("totalAmount", order.getTotalAmount());

        return result;
    }
}
