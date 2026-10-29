package com.naijamart.service;

import com.naijamart.entity.Product;
import com.naijamart.entity.StockNotification;
import com.naijamart.repository.ProductRepository;
import com.naijamart.repository.StockNotificationRepository;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class StockNotificationService {

    private final StockNotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final JavaMailSender mailSender;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public StockNotificationService(
            StockNotificationRepository notificationRepository,
            ProductRepository productRepository,
            JavaMailSender mailSender) {

        this.notificationRepository = notificationRepository;
        this.productRepository = productRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public StockNotification registerNotification(
            Long productId,
            String customerEmail) {

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Please provide your email address");
        }

        String email = customerEmail.trim().toLowerCase(Locale.ROOT);

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException(
                    "Please provide a valid email address");
        }

        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        if (product.getStockQuantity() != null
                && product.getStockQuantity() > 0) {
            throw new IllegalArgumentException(
                    "This product is currently in stock");
        }

        if (notificationRepository
                .existsByProduct_IdAndCustomerEmailIgnoreCase(
                        productId, email)) {
            throw new IllegalArgumentException(
                    "You have already requested a stock notification");
        }

        StockNotification notification = new StockNotification();
        notification.setProduct(product);
        notification.setCustomerEmail(email);

        return notificationRepository.save(notification);
    }

    @Transactional
    public int notifyWaitingCustomers(Product product) {

        if (product == null
                || product.getId() == null
                || product.getStockQuantity() == null
                || product.getStockQuantity() <= 0) {
            return 0;
        }

        List<StockNotification> waitingCustomers =
                notificationRepository
                        .findByProduct_IdAndNotifiedFalse(product.getId());

        int emailsSent = 0;

        for (StockNotification notification : waitingCustomers) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();

                message.setTo(notification.getCustomerEmail());
                message.setSubject(
                        "NaijaMart: " + product.getName()
                                + " is back in stock");

                message.setText(
                        "Hello,\n\n"
                        + "Good news! The product you were interested in "
                        + "is now available on NaijaMart.\n\n"
                        + "Product: " + product.getName() + "\n"
                        + "Available quantity: "
                        + product.getStockQuantity() + "\n\n"
                        + "Visit NaijaMart to place your order.\n\n"
                        + "Thank you for shopping with NaijaMart.");

                mailSender.send(message);

                notification.setNotified(true);
                notificationRepository.save(notification);
                emailsSent++;

            } catch (MailException exception) {
                System.err.println(
                        "Could not send stock notification to "
                        + notification.getCustomerEmail()
                        + ": " + exception.getMessage());
            }
        }

        return emailsSent;
    }
}