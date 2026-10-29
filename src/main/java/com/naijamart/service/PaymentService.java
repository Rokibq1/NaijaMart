
package com.naijamart.service;

import com.naijamart.entity.Product;
import com.naijamart.order.OrderItem;
import com.naijamart.order.OrderStatus;
import com.naijamart.order.PaymentStatus;
import com.naijamart.order.PurchaseOrder;
import com.naijamart.order.PurchaseOrderRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PurchaseOrderRepository orderRepository;
    private final RestClient restClient;
    private final String secretKey;
    private final String currency;
    private final String callbackUrl;

    public PaymentService(
            PurchaseOrderRepository orderRepository,
            @Value("${paystack.secret-key:}") String secretKey,
            @Value("${paystack.base-url:https://api.paystack.co}") String baseUrl,
            @Value("${paystack.currency:NGN}") String currency,
            @Value("${paystack.callback-url:http://localhost:8080/?payment=callback}") String callbackUrl) {

        this.orderRepository = orderRepository;
        this.secretKey = secretKey;
        this.currency = currency;
        this.callbackUrl = callbackUrl;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(
                        "Authorization",
                        "Bearer " + secretKey)
                .defaultHeader(
                        "Content-Type",
                        "application/json")
                .build();
    }

    public Map<String, Object> initializePayment(Long orderId) {

        checkSecretKey();

        PurchaseOrder order = orderRepository
                .findOrderWithItemsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order cannot be paid.");
        }

        String reference = "NM-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();

        order.setPaymentReference(reference);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        // Amount must be in kobo, not naira.
        long amountInKobo = order.getTotalAmount()
                .movePointRight(2)
                .longValueExact();

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", order.getCustomerEmail());
        payload.put("amount", amountInKobo);
        payload.put("currency", currency);
        payload.put("reference", reference);
        payload.put("callback_url", callbackUrl);

        Map<String, Object> response;

        try {
            response = restClient.post()
                    .uri("/transaction/initialize")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not initialize Paystack test payment. "
                            + "Check your test key and try again.");
        }

        if (response == null
                || !Boolean.TRUE.equals(response.get("status"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Paystack could not initialize the transaction.");
        }

        Map<String, Object> data = asMap(response.get("data"));

        Object authorizationUrl = data.get("authorization_url");
        Object returnedReference = data.get("reference");

        if (authorizationUrl == null
                || returnedReference == null
                || !reference.equals(returnedReference.toString())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Paystack returned an invalid payment response.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("trackingNumber", order.getTrackingNumber());
        result.put("reference", reference);
        result.put("authorizationUrl", authorizationUrl);
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("amount", order.getTotalAmount());
        result.put("currency", currency);

        return result;
    }

    @Transactional
    public Map<String, Object> verifyPayment(
            Long orderId, String reference) {

        checkSecretKey();

        if (reference == null || reference.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment reference is required.");
        }

        PurchaseOrder order = orderRepository
                .findOrderWithItemsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));

        if (order.getPaymentReference() == null
                || !order.getPaymentReference().equals(reference)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment reference does not match this order.");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return paymentResult(order, "Payment was already verified.");
        }

        Map<String, Object> response;

        try {
            response = restClient.get()
                    .uri("/transaction/verify/{reference}", reference)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not verify the Paystack transaction. "
                            + "Please try again.");
        }

        if (response == null
                || !Boolean.TRUE.equals(response.get("status"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Paystack could not verify this transaction.");
        }

        Map<String, Object> data = asMap(response.get("data"));

        String transactionStatus =
                String.valueOf(data.get("status"));

        String returnedReference =
                String.valueOf(data.get("reference"));

        String returnedCurrency =
                String.valueOf(data.get("currency"));

        Object amountValue = data.get("amount");

        long expectedAmount = order.getTotalAmount()
                .movePointRight(2)
                .longValueExact();

        boolean amountMatches =
                amountValue instanceof Number
                        && ((Number) amountValue).longValue()
                        == expectedAmount;

        boolean transactionMatches =
                reference.equals(returnedReference)
                        && currency.equals(returnedCurrency)
                        && amountMatches;

        if ("success".equalsIgnoreCase(transactionStatus)
                && transactionMatches) {

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);

            orderRepository.save(order);

            return paymentResult(
                    order,
                    "Test payment verified successfully.");
        }

        if ("failed".equalsIgnoreCase(transactionStatus)
                || "abandoned".equalsIgnoreCase(transactionStatus)) {

            releaseStockAndMarkFailed(order);

            return paymentResult(
                    order,
                    "Payment failed or was abandoned. "
                            + "Please start checkout again.");
        }

        if ("success".equalsIgnoreCase(transactionStatus)
                && !transactionMatches) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment details did not match the order.");
        }

        return paymentResult(
                order,
                "Payment is not yet confirmed. Please check again.");
    }

    private void releaseStockAndMarkFailed(PurchaseOrder order) {

        if (order.getPaymentStatus() != PaymentStatus.FAILED
                && order.getStatus() != OrderStatus.CANCELLED) {

            for (OrderItem item : order.getItems()) {

                Product product = item.getProduct();

                int currentStock = product.getStockQuantity() == null
                        ? 0
                        : product.getStockQuantity();

                try {
                    product.setStockQuantity(
                            Math.addExact(
                                    currentStock,
                                    item.getQuantity()));
                } catch (ArithmeticException exception) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Unable to restore product stock.");
                }
            }

            order.setStatus(OrderStatus.CANCELLED);
        }

        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
    }

    private Map<String, Object> paymentResult(
            PurchaseOrder order, String message) {

        Map<String, Object> result = new HashMap<>();
        result.put("message", message);
        result.put("orderId", order.getId());
        result.put("trackingNumber", order.getTrackingNumber());
        result.put("orderStatus", order.getStatus());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("totalAmount", order.getTotalAmount());

        return result;
    }

    private Map<String, Object> asMap(Object value) {

        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();

        source.forEach((key, item) ->
                result.put(String.valueOf(key), item));

        return result;
    }

    private void checkSecretKey() {

        if (secretKey == null
                || !secretKey.startsWith("sk_test_")) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A Paystack test secret key must be configured.");
        }
    }
}
