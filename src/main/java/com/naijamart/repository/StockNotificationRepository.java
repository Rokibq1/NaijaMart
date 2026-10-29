package com.naijamart.repository;

import com.naijamart.entity.StockNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockNotificationRepository
        extends JpaRepository<StockNotification, Long> {

    boolean existsByProduct_IdAndCustomerEmailIgnoreCase(
            Long productId,
            String customerEmail
    );

    List<StockNotification> findByProduct_IdAndNotifiedFalse(
            Long productId
    );
}