package com.naijamart.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentProofRepository
        extends JpaRepository<PaymentProof, Long> {

    Optional<PaymentProof> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order"})
    List<PaymentProof>
    findAllByOrderPaymentStatusOrderBySubmittedAtAsc(
            PaymentStatus paymentStatus
    );
}