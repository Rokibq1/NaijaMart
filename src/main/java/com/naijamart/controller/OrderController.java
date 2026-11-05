
package com.naijamart.controller;

import com.naijamart.dto.CheckoutRequest;
import com.naijamart.dto.OrderStatusRequest;
import com.naijamart.order.OrderItem;
import com.naijamart.order.OrderStatus;
import com.naijamart.order.PaymentStatus;
import com.naijamart.order.PurchaseOrder;
import com.naijamart.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // CUSTOMER CHECKOUT - LOGIN REQUIRED
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> placeOrder(
            @Valid @RequestBody CheckoutRequest request) {

        PurchaseOrder order = orderService.placeOrder(request);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("message", "Your order was placed successfully.");
        response.put("orderId", order.getId());
        response.put("trackingNumber", order.getTrackingNumber());
        response.put("status", order.getStatus());
        response.put("totalAmount", order.getTotalAmount());
        response.put("createdAt", order.getCreatedAt());

        response.put("emailSent", false);
        response.put(
                "emailMessage",
                "Your order was placed successfully. "
                        + "The confirmation email is being processed.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // CUSTOMER ORDER TRACKING
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<Map<String, Object>> trackOrder(
            @PathVariable String trackingNumber) {

        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tracking number is required.");
        }

        PurchaseOrder order =
                orderService.getOrderByTrackingNumber(trackingNumber);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("trackingNumber", order.getTrackingNumber());
        response.put("status", order.getStatus());
        response.put("totalAmount", order.getTotalAmount());
        response.put("createdAt", order.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    // CUSTOMER: CHECK OWN PAYMENT STATUS
    @GetMapping("/{trackingNumber}/payment-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCustomerPaymentStatus(
            @PathVariable String trackingNumber,
            Principal principal) {

        PaymentStatus paymentStatus =
                orderService.getCustomerPaymentStatus(
                        trackingNumber,
                        principal.getName());

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("trackingNumber", trackingNumber);
        response.put("paymentStatus", paymentStatus);

        return ResponseEntity.ok(response);
    }

    // ADMIN: VIEW ALL ORDERS
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllOrdersForAdmin() {

        List<Map<String, Object>> orders = orderService
                .getAllOrdersForAdmin()
                .stream()
                .map(this::toAdminOrderResponse)
                .toList();

        return ResponseEntity.ok(orders);
    }

    // ADMIN: UPDATE ORDER STATUS
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request) {

        OrderStatus newStatus;

        try {
            newStatus = OrderStatus.valueOf(
                    request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order status.");
        }

        PurchaseOrder order =
                orderService.updateOrderStatus(id, newStatus);

        return ResponseEntity.ok(toAdminOrderResponse(order));
    }

    // FORMAT ORDER DETAILS FOR THE ADMIN DASHBOARD
    private Map<String, Object> toAdminOrderResponse(
            PurchaseOrder order) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", order.getId());
        response.put("trackingNumber", order.getTrackingNumber());
        response.put("customerName", order.getCustomerName());
        response.put("customerEmail", order.getCustomerEmail());
        response.put("customerPhone", order.getCustomerPhone());
        response.put("deliveryAddress", order.getDeliveryAddress());
        response.put("totalAmount", order.getTotalAmount());
        response.put("status", order.getStatus());

        // Include payment information for administrators.
        response.put("paymentStatus", order.getPaymentStatus());
        response.put("paymentReference", order.getPaymentReference());
        response.put("receiptNumber", order.getReceiptNumber());

        response.put("createdAt", order.getCreatedAt());

        List<Map<String, Object>> items = order.getItems()
                .stream()
                .map(this::toOrderItemResponse)
                .toList();

        response.put("items", items);

        return response;
    }

    // FORMAT EACH PRODUCT IN AN ORDER
    private Map<String, Object> toOrderItemResponse(OrderItem item) {

        Map<String, Object> itemData = new LinkedHashMap<>();

        itemData.put("productId", item.getProduct().getId());
        itemData.put("productName", item.getProduct().getName());
        itemData.put("quantity", item.getQuantity());
        itemData.put("unitPrice", item.getUnitPrice());
        itemData.put("subtotal", item.getSubtotal());

        return itemData;
    }
}
