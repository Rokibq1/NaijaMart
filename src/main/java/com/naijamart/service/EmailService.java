
package com.naijamart.service;

import com.naijamart.order.OrderItem;
import com.naijamart.order.PurchaseOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderConfirmation(PurchaseOrder order) {
        SimpleMailMessage message = new SimpleMailMessage();

        StringBuilder emailBody = new StringBuilder();

        emailBody.append("Hello ")
                .append(order.getCustomerName())
                .append(",\n\n");

        emailBody.append("Thank you for shopping with NaijaMart!\n");
        emailBody.append("Your order has been placed successfully.\n\n");

        emailBody.append("ORDER DETAILS\n");
        emailBody.append("------------------------------\n");
        emailBody.append("Order ID: ")
                .append(order.getId()).append("\n");

        emailBody.append("Tracking Number: ")
                .append(order.getTrackingNumber()).append("\n");

        emailBody.append("Order Status: ")
                .append(order.getStatus()).append("\n\n");

        emailBody.append("ITEMS ORDERED\n");
        emailBody.append("------------------------------\n");

        for (OrderItem item : order.getItems()) {
            emailBody.append("Product: ")
                    .append(item.getProduct().getName()).append("\n");

            emailBody.append("Quantity: ")
                    .append(item.getQuantity()).append("\n");

            emailBody.append("Unit Price: NGN ")
                    .append(item.getUnitPrice()).append("\n");

            emailBody.append("Subtotal: NGN ")
                    .append(item.getSubtotal()).append("\n\n");
        }

        emailBody.append("Total Amount: NGN ")
                .append(order.getTotalAmount()).append("\n\n");

        emailBody.append("Delivery Address: ")
                .append(order.getDeliveryAddress()).append("\n\n");

        emailBody.append("Keep your tracking number safe so you can ")
                .append("check your order status.\n\n");

        emailBody.append("Thank you for choosing NaijaMart!\n");
        emailBody.append("Happy shopping!\n");
        emailBody.append("The NaijaMart Team");

        message.setFrom(senderEmail);
        message.setTo(order.getCustomerEmail());
        message.setSubject(
                "NaijaMart Order Confirmation - "
                        + order.getTrackingNumber()
        );
        message.setText(emailBody.toString());

        try {
            mailSender.send(message);

            logger.info(
                    "Order confirmation email sent for tracking number {}",
                    order.getTrackingNumber()
            );
        } catch (MailException exception) {
            logger.error(
                    "Could not send confirmation email for order {}",
                    order.getTrackingNumber(),
                    exception
            );

            throw exception;
        }
    }
}
