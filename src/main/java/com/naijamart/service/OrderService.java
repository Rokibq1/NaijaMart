
package com.naijamart.service;

import com.naijamart.dto.CheckoutRequest;
import com.naijamart.entity.Product;
import com.naijamart.event.OrderPlacedEvent;
import com.naijamart.order.OrderItem;
import com.naijamart.order.OrderStatus;
import com.naijamart.order.PaymentStatus;
import com.naijamart.order.PurchaseOrder;
import com.naijamart.order.PurchaseOrderRepository;
import com.naijamart.repository.ProductRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final PurchaseOrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            ProductRepository productRepository,
            PurchaseOrderRepository orderRepository,
            ApplicationEventPublisher eventPublisher) {

        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    // CUSTOMER CHECKOUT
    @Transactional
    public PurchaseOrder placeOrder(CheckoutRequest request) {

        PurchaseOrder order = new PurchaseOrder();

        order.setCustomerName(request.getCustomerName().trim());

        order.setCustomerEmail(
                request.getCustomerEmail().trim().toLowerCase());

        order.setCustomerPhone(request.getCustomerPhone().trim());

        order.setDeliveryAddress(
                request.getDeliveryAddress().trim());

        order.setTrackingNumber(
                "NM-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase());

        BigDecimal total = BigDecimal.ZERO;

        for (CheckoutRequest.OrderLineRequest line : request.getItems()) {

            Product product = productRepository
                    .findActiveProductForUpdate(line.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "A requested product was not found or is inactive."));

            int requestedQuantity = line.getQuantity();

            if (product.getStockQuantity() == null
                    || requestedQuantity <= 0
                    || requestedQuantity > product.getStockQuantity()) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock or invalid quantity for "
                                + product.getName());
            }

            BigDecimal unitPrice = product.getPrice();

            OrderItem item = new OrderItem(
                    product,
                    requestedQuantity,
                    unitPrice);

            order.addItem(item);

            total = total.add(
                    unitPrice.multiply(
                            BigDecimal.valueOf(requestedQuantity)));

            // Reserve stock for this pending order.
            product.setStockQuantity(
                    product.getStockQuantity() - requestedQuantity);
        }

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);

        // Save the order and its items.
        PurchaseOrder savedOrder = orderRepository.saveAndFlush(order);

        // Publish the event for the order confirmation email.
        eventPublisher.publishEvent(
                new OrderPlacedEvent(savedOrder.getId()));

        return savedOrder;
    }

    // CUSTOMER ORDER TRACKING
    @Transactional(readOnly = true)
    public PurchaseOrder getOrderByTrackingNumber(
            String trackingNumber) {

        return orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));
    }

    // CUSTOMER: GET OWN PAYMENT STATUS
    @Transactional(readOnly = true)
    public PaymentStatus getCustomerPaymentStatus(
            String trackingNumber,
            String customerEmail) {

        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tracking number is required.");
        }

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Customer authentication is required.");
        }

        PurchaseOrder order = orderRepository
                .findByTrackingNumberAndCustomerEmail(
                        trackingNumber.trim(),
                        customerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));

        return order.getPaymentStatus();
    }

    // ADMIN: RETURN ALL ORDERS, NEWEST FIRST
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getAllOrdersForAdmin() {

        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    // ADMIN: UPDATE ORDER STATUS
    @Transactional
    public PurchaseOrder updateOrderStatus(
            Long orderId,
            OrderStatus newStatus) {

        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid order status is required.");
        }

        PurchaseOrder order = orderRepository
                .findOrderWithItemsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));

        OrderStatus currentStatus = order.getStatus();

        // A cancelled order cannot be changed.
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A cancelled order cannot be changed.");
        }

        // A delivered order cannot move backwards.
        if (currentStatus == OrderStatus.DELIVERED
                && newStatus != OrderStatus.DELIVERED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A delivered order cannot be changed.");
        }

        // Paid orders cannot be cancelled without a separate refund process.
        if (newStatus == OrderStatus.CANCELLED
                && order.getPaymentStatus() == PaymentStatus.PAID) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid. "
                            + "A refund must be handled separately.");
        }

        // Restore stock when an unpaid order is cancelled.
        if (newStatus == OrderStatus.CANCELLED) {

            for (OrderItem item : order.getItems()) {

                Product product = item.getProduct();

                int currentStock = product.getStockQuantity() == null
                        ? 0
                        : product.getStockQuantity();

                int restoredStock;

                try {
                    restoredStock = Math.addExact(
                            currentStock,
                            item.getQuantity());

                } catch (ArithmeticException exception) {

                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Stock quantity exceeds the allowed limit.");
                }

                product.setStockQuantity(restoredStock);

                productRepository.save(product);
            }
        }

        order.setStatus(newStatus);

        return orderRepository.save(order);
    }
}
