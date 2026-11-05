
package com.naijamart.service;

import com.naijamart.order.OrderStatus;
import com.naijamart.order.PaymentProof;
import com.naijamart.order.PaymentProofRepository;
import com.naijamart.order.PaymentStatus;
import com.naijamart.order.PurchaseOrder;
import com.naijamart.order.PurchaseOrderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class TransferPaymentService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final PaymentProofRepository proofRepository;
    private final PurchaseOrderRepository orderRepository;

    public TransferPaymentService(
            PaymentProofRepository proofRepository,
            PurchaseOrderRepository orderRepository) {

        this.proofRepository = proofRepository;
        this.orderRepository = orderRepository;
    }

    // CUSTOMER: Submit bank transfer proof
    @Transactional
    public PaymentProof submitProof(
            Long orderId,
            MultipartFile file,
            String customerEmail) {

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in.");
        }

        PurchaseOrder order = orderRepository.findOrderWithItemsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));

        if (order.getCustomerEmail() == null
                || !order.getCustomerEmail().equalsIgnoreCase(customerEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot submit payment proof for this order.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.DELIVERED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment proof cannot be submitted for this order.");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid.");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select a transfer receipt.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The transfer receipt must not exceed 5 MB.");
        }

        String contentType = file.getContentType();

        if (!"image/jpeg".equalsIgnoreCase(contentType)
                && !"image/png".equalsIgnoreCase(contentType)
                && !"application/pdf".equalsIgnoreCase(contentType)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPG, PNG and PDF files are accepted.");
        }

        byte[] fileBytes;

        try {
            fileBytes = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The uploaded file could not be read.");
        }

        validateFileSignature(fileBytes, contentType);

        PaymentProof proof = proofRepository.findByOrderId(orderId)
                .orElseGet(PaymentProof::new);

        proof.setOrder(order);
        proof.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        proof.setContentType(contentType);
        proof.setFileSize(file.getSize());
        proof.setProofData(fileBytes);

        // Reset previous review details if customer resubmits
        proof.setReviewedAt(null);
        proof.setReviewedBy(null);
        proof.setRejectionReason(null);

        order.setPaymentStatus(PaymentStatus.PENDING);

        orderRepository.save(order);

        return proofRepository.save(proof);
    }

    // ADMIN: View all submitted proofs awaiting review
    @Transactional(readOnly = true)
    public List<PaymentProof> getPendingProofs() {

        return proofRepository
                .findAllByOrderPaymentStatusOrderBySubmittedAtAsc(
                        PaymentStatus.PENDING);
    }

    // ADMIN: Open a particular transfer proof
    @Transactional(readOnly = true)
    public PaymentProof getProofForAdmin(Long orderId) {

        return proofRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment proof not found."));
    }

    // ADMIN: Approve a bank transfer
    @Transactional
    public PurchaseOrder approvePayment(
            Long orderId,
            String adminEmail) {

        PaymentProof proof = getProofForAdmin(orderId);
        PurchaseOrder order = proof.getOrder();

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This payment is not awaiting approval.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A cancelled order cannot be approved.");
        }

        // Confirm payment only after admin verification
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);

        // Generate bank payment reference
        order.setPaymentReference(
                "BANK-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase()
        );

        // Generate official receipt number
        order.setReceiptNumber(
                "NM-RCP-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase()
        );

        // Record admin review
        proof.setReviewedAt(LocalDateTime.now());
        proof.setReviewedBy(adminEmail);

        proofRepository.save(proof);

        return orderRepository.save(order);
    }

    // ADMIN: Reject a bank transfer
    @Transactional
    public PurchaseOrder rejectPayment(
            Long orderId,
            String adminEmail,
            String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a reason for rejecting the proof.");
        }

        if (reason.trim().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The rejection reason must not exceed 500 characters.");
        }

        PaymentProof proof = getProofForAdmin(orderId);
        PurchaseOrder order = proof.getOrder();

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This payment is not awaiting review.");
        }

        order.setPaymentStatus(PaymentStatus.REJECTED);

        proof.setReviewedAt(LocalDateTime.now());
        proof.setReviewedBy(adminEmail);
        proof.setRejectionReason(reason.trim());

        proofRepository.save(proof);

        return orderRepository.save(order);
    }

    // CUSTOMER: Check payment status for their own order
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerPaymentStatus(
            Long orderId,
            String customerEmail) {

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in.");
        }

        PurchaseOrder order = orderRepository.findOrderWithItemsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));

        if (order.getCustomerEmail() == null
                || !customerEmail.equalsIgnoreCase(order.getCustomerEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view this order.");
        }

        Map<String, Object> response = new HashMap<>();

        response.put("orderId", order.getId());
        response.put("trackingNumber", order.getTrackingNumber());
        response.put("paymentStatus", order.getPaymentStatus().name());
        response.put("orderStatus", order.getStatus().name());

        PaymentProof proof = proofRepository.findByOrderId(orderId)
                .orElse(null);

        if (proof != null) {
            response.put("submittedAt", proof.getSubmittedAt());
            response.put("rejectionReason", proof.getRejectionReason());
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            response.put("receiptNumber", order.getReceiptNumber());
        }

        return response;
    }

    // CUSTOMER: Get an order for its approved PDF receipt
    @Transactional(readOnly = true)
    public PurchaseOrder getOrderForReceipt(
            Long orderId,
            String customerEmail) {

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in to download your receipt.");
        }

        PurchaseOrder order = orderRepository
                .findOrderWithItemsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found."));

        if (order.getCustomerEmail() == null
                || !customerEmail.equalsIgnoreCase(
                        order.getCustomerEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this receipt.");
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Your receipt will be available after payment approval.");
        }

        if (order.getReceiptNumber() == null
                || order.getReceiptNumber().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Receipt number has not been generated.");
        }

        return order;
    }

    // Validate actual uploaded file signature
    private void validateFileSignature(
            byte[] data,
            String contentType) {

        boolean valid = false;

        if ("application/pdf".equalsIgnoreCase(contentType)) {
            valid = data.length >= 5
                    && new String(data, 0, 5, StandardCharsets.US_ASCII)
                            .equals("%PDF-");
        }

        if ("image/png".equalsIgnoreCase(contentType)) {
            valid = data.length >= 8
                    && (data[0] & 0xFF) == 0x89
                    && data[1] == 0x50
                    && data[2] == 0x4E
                    && data[3] == 0x47
                    && data[4] == 0x0D
                    && data[5] == 0x0A
                    && data[6] == 0x1A
                    && data[7] == 0x0A;
        }

        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            valid = data.length >= 3
                    && (data[0] & 0xFF) == 0xFF
                    && (data[1] & 0xFF) == 0xD8
                    && (data[2] & 0xFF) == 0xFF;
        }

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The uploaded file does not match its file type.");
        }
    }

    // Sanitize uploaded filename
    private String safeFilename(String filename) {

        if (filename == null || filename.isBlank()) {
            return "transfer-proof";
        }

        String safe = filename.replace("\\", "/");

        safe = safe.substring(safe.lastIndexOf('/') + 1);

        safe = safe.replaceAll("[\\r\\n\\p{Cntrl}]", "_");

        return safe.length() > 255
                ? safe.substring(0, 255)
                : safe;
    }
}
