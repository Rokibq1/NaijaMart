
package com.naijamart.controller;

import com.naijamart.order.PaymentProof;
import com.naijamart.order.PurchaseOrder;
import com.naijamart.service.EmailNotificationService;
import com.naijamart.service.ReceiptService;
import com.naijamart.service.TransferPaymentService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
public class TransferPaymentController {

    private final TransferPaymentService transferPaymentService;
    private final ReceiptService receiptService;
    private final EmailNotificationService emailNotificationService;

    @Value("${naijamart.bank.account-name:}")
    private String bankAccountName;

    @Value("${naijamart.bank.bank-name:}")
    private String bankName;

    @Value("${naijamart.bank.account-number:}")
    private String bankAccountNumber;

    public TransferPaymentController(
            TransferPaymentService transferPaymentService,
            ReceiptService receiptService,
            EmailNotificationService emailNotificationService) {

        this.transferPaymentService = transferPaymentService;
        this.receiptService = receiptService;
        this.emailNotificationService = emailNotificationService;
    }

    // CUSTOMER: Get bank account details
    @GetMapping("/bank-details")
    public ResponseEntity<?> getBankDetails() {

        Map<String, Object> response = new HashMap<>();

        response.put("accountName", bankAccountName);
        response.put("bankName", bankName);
        response.put("accountNumber", bankAccountNumber);

        response.put(
                "configured",
                bankAccountName != null && !bankAccountName.isBlank()
                && bankName != null && !bankName.isBlank()
                && bankAccountNumber != null && !bankAccountNumber.isBlank()
        );

        return ResponseEntity.ok(response);
    }

    // CUSTOMER: Upload bank transfer proof
    @PostMapping(
            value = "/orders/{orderId}/proof",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> submitProof(
            @PathVariable Long orderId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        PaymentProof proof = transferPaymentService.submitProof(
                orderId,
                file,
                authentication.getName()
        );

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Payment proof submitted successfully.");
        response.put("paymentStatus", "PENDING");
        response.put("submittedAt", proof.getSubmittedAt());

        return ResponseEntity.ok(response);
    }

    // CUSTOMER: Check payment status
    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable Long orderId,
            Authentication authentication) {

        return ResponseEntity.ok(
                transferPaymentService.getCustomerPaymentStatus(
                        orderId,
                        authentication.getName()
                )
        );
    }

    // ADMIN: View all pending payment proofs
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingProofs() {

        List<PaymentProof> proofs =
                transferPaymentService.getPendingProofs();

        List<Map<String, Object>> response = proofs.stream().map(proof -> {

            PurchaseOrder order = proof.getOrder();

            Map<String, Object> item = new HashMap<>();

            item.put("orderId", order.getId());
            item.put("trackingNumber", order.getTrackingNumber());
            item.put("customerName", order.getCustomerName());
            item.put("customerEmail", order.getCustomerEmail());
            item.put("amount", order.getTotalAmount());
            item.put("filename", proof.getOriginalFilename());
            item.put("submittedAt", proof.getSubmittedAt());
            item.put("paymentStatus", order.getPaymentStatus().name());

            return item;

        }).toList();

        return ResponseEntity.ok(response);
    }

    // ADMIN: View an uploaded payment proof
    @GetMapping("/admin/{orderId}/proof")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ByteArrayResource> viewProof(
            @PathVariable Long orderId) {

        PaymentProof proof =
                transferPaymentService.getProofForAdmin(orderId);

        ByteArrayResource resource =
                new ByteArrayResource(proof.getProofData());

        MediaType contentType;

        try {
            contentType = MediaType.parseMediaType(proof.getContentType());
        } catch (Exception exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(proof.getFileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(proof.getOriginalFilename())
                                .build()
                                .toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    // ADMIN: Approve a bank transfer
    @PostMapping("/admin/{orderId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approvePayment(
            @PathVariable Long orderId,
            Authentication authentication) {

        PurchaseOrder order =
                transferPaymentService.approvePayment(
                        orderId,
                        authentication.getName()
                );

        emailNotificationService.sendPaymentApproved(order);

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Payment approved successfully.");
        response.put("trackingNumber", order.getTrackingNumber());
        response.put("paymentStatus", order.getPaymentStatus().name());
        response.put("orderStatus", order.getStatus().name());
        response.put("paymentReference", order.getPaymentReference());
        response.put("receiptNumber", order.getReceiptNumber());

        return ResponseEntity.ok(response);
    }

    // ADMIN: Reject a bank transfer
    @PostMapping("/admin/{orderId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectPayment(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String reason = request.get("reason");

        // Correct argument order: orderId, adminEmail, reason
        PurchaseOrder order =
                transferPaymentService.rejectPayment(
                        orderId,
                        authentication.getName(),
                        reason
                );

        emailNotificationService.sendPaymentRejected(order, reason);

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Payment rejected.");
        response.put("trackingNumber", order.getTrackingNumber());
        response.put("paymentStatus", order.getPaymentStatus().name());

        return ResponseEntity.ok(response);
    }

    // CUSTOMER: Download receipt after payment approval
    @GetMapping("/orders/{orderId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable Long orderId,
            Authentication authentication) {

        byte[] pdf = receiptService.generateReceipt(
                orderId,
                authentication.getName()
        );

        PurchaseOrder order =
                transferPaymentService.getOrderForReceipt(
                        orderId,
                        authentication.getName()
                );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(order.getReceiptNumber() + ".pdf")
                                .build()
                                .toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(pdf);
    }
}
