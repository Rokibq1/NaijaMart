
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            readOnly = true)
    public void sendOrderConfirmation(OrderPlacedEvent event) {

        if (senderEmail == null || senderEmail.isBlank()) {
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
                    .append(" | Unit price: ₦")
                    .append(item.getUnitPrice())
                    .append(" | Subtotal: ₦")
                    .append(item.getSubtotal())
                    .append("\n");
        }

        body.append("\nTotal amount: ₦")
                .append(order.getTotalAmount())
                .append("\n");

        body.append("Delivery address: ")
                .append(order.getDeliveryAddress())
                .append("\n\n");

        body.append("Keep your tracking number for future reference.\n");
        body.append("Thank you for choosing NaijaMart!");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(order.getCustomerEmail());
        message.setSubject(
                "NaijaMart Order Confirmation - "
                        + order.getTrackingNumber());
        message.setText(body.toString());

        try {
            mailSender.send(message);
            log.info("Order confirmation email sent for order {}.",
                    order.getId());
        } catch (MailException exception) {
            log.error(
                    "Order {} was saved, but its confirmation email failed.",
                    order.getId(), exception);
        }
    }
}
