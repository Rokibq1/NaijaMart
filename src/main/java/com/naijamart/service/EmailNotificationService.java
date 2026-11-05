
package com.naijamart.service;

import com.naijamart.order.OrderItem;
import com.naijamart.order.PurchaseOrder;
import com.naijamart.order.PurchaseOrderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.naijamart.event.OrderPlacedEvent;

@Service
public class EmailNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final PurchaseOrderRepository orderRepository;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public EmailNotificationService(
            JavaMailSender mailSender,
            PurchaseOrderRepository orderRepository) {

        this.mailSender = mailSender;
        this.orderRepository = orderRepository;
    }

    // ORIGINAL: Send order confirmation after checkout
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            readOnly = true)
    public void sendOrderConfirmation(OrderPlacedEvent event) {

        if (!isEmailConfigured()) {
            log.warn("Order email not sent: MAIL_USERNAME is not configured.");
            return;
        }

        PurchaseOrder order = orderRepository.findById(event.getOrderId())
                .orElse(null);

        if (order == null) {
            log.warn("Order email not sent: order {} was not found.",
                    event.getOrderId());
            return;
        }

        StringBuilder body = new StringBuilder();

        body.append("Hello ")
                .append(order.getCustomerName())
                .append(",\n\n");

        body.append("Thank you for shopping with NaijaMart!\n");
        body.append("Your order has been received successfully.\n\n");

        body.append("ORDER DETAILS\n");
        body.append("------------------------------\n");
        body.append("Order ID: ").append(order.getId()).append("\n");
        body.append("Tracking number: ")
                .append(order.getTrackingNumber()).append("\n");
        body.append("Status: ").append(order.getStatus()).append("\n\n");

        body.append("ITEMS ORDERED\n");

        for (OrderItem item : order.getItems()) {
            body.append("- ")
                    .append(item.getProduct().getName())
                    .append(" | Quantity: ")
                    .append(item.getQuantity())
                    .append(" | Unit price: NGN ")
                    .append(item.getUnitPrice())
                    .append(" | Subtotal: NGN ")
                    .append(item.getSubtotal())
                    .append("\n");
        }

        body.append("\nTotal amount: NGN ")
                .append(order.getTotalAmount())
                .append("\n");

        body.append("Delivery address: ")
                .append(order.getDeliveryAddress())
                .append("\n\n");

        body.append("Keep your tracking number for future reference.\n");
        body.append("Thank you for choosing NaijaMart!");

        sendEmail(
                order.getCustomerEmail(),
                "NaijaMart Order Confirmation - "
                        + order.getTrackingNumber(),
                body.toString(),
                order.getId()
        );
    }

    // NEW: Notify customer when bank transfer is approved
    public void sendPaymentApproved(PurchaseOrder order) {

        if (order == null) {
            return;
        }

        StringBuilder body = new StringBuilder();

        body.append("Hello ")
                .append(order.getCustomerName())
                .append(",\n\n");

        body.append("Good news! Your bank transfer has been verified ");
        body.append("and approved by the NaijaMart administrator.\n\n");

        body.append("PAYMENT CONFIRMATION\n");
        body.append("--------------------------------\n");

        body.append("Order ID: ")
                .append(order.getId()).append("\n");

        body.append("Tracking Number: ")
                .append(order.getTrackingNumber()).append("\n");

        body.append("Receipt Number: ")
                .append(order.getReceiptNumber()).append("\n");

        body.append("Payment Reference: ")
                .append(order.getPaymentReference()).append("\n");

        body.append("Amount Paid: NGN ")
                .append(order.getTotalAmount()).append("\n");

        body.append("Payment Status: PAID\n");
        body.append("Order Status: ")
                .append(order.getStatus()).append("\n\n");

        body.append("Your official PDF receipt is now available ");
        body.append("for download from your NaijaMart account.\n\n");

        body.append("Thank you for shopping with NaijaMart!");

        sendEmail(
                order.getCustomerEmail(),
                "NaijaMart Payment Approved - "
                        + order.getTrackingNumber(),
                body.toString(),
                order.getId()
        );
    }

    // NEW: Notify customer when bank transfer is rejected
    public void sendPaymentRejected(
            PurchaseOrder order,
            String reason) {

        if (order == null) {
            return;
        }

        StringBuilder body = new StringBuilder();

        body.append("Hello ")
                .append(order.getCustomerName())
                .append(",\n\n");

        body.append("We have reviewed the bank transfer proof ");
        body.append("submitted for your NaijaMart order.\n\n");

        body.append("Unfortunately, we could not approve the payment.\n\n");

        body.append("ORDER DETAILS\n");
        body.append("--------------------------------\n");

        body.append("Tracking Number: ")
                .append(order.getTrackingNumber()).append("\n");

        body.append("Amount: NGN ")
                .append(order.getTotalAmount()).append("\n");

        body.append("Payment Status: REJECTED\n\n");

        body.append("Reason for rejection:\n");
        body.append(reason == null ? "Please contact NaijaMart support." : reason);
        body.append("\n\n");

        body.append("Please review the reason and contact our support ");
        body.append("team if you need assistance with submitting ");
        body.append("a new transfer proof.\n\n");

        body.append("Thank you for choosing NaijaMart.");

        sendEmail(
                order.getCustomerEmail(),
                "NaijaMart Payment Review - "
                        + order.getTrackingNumber(),
                body.toString(),
                order.getId()
        );
    }

    // COMMON: Send email safely
    private void sendEmail(
            String recipient,
            String subject,
            String body,
            Long orderId) {

        if (!isEmailConfigured()) {
            log.warn("Email not sent for order {}: MAIL_USERNAME is not configured.",
                    orderId);
            return;
        }

        if (recipient == null || recipient.isBlank()) {
            log.warn("Email not sent for order {}: customer email is missing.",
                    orderId);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);

            log.info("Email successfully sent for order {}.", orderId);

        } catch (MailException exception) {

            log.error(
                    "Order {} was processed, but its email notification failed.",
                    orderId,
                    exception
            );
        }
    }

    private boolean isEmailConfigured() {
        return senderEmail != null && !senderEmail.isBlank();
    }
}
