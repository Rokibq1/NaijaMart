
package com.naijamart.service;

import com.naijamart.order.OrderItem;
import com.naijamart.order.PaymentProof;
import com.naijamart.order.PaymentStatus;
import com.naijamart.order.PurchaseOrder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    private final TransferPaymentService transferPaymentService;

    public ReceiptService(TransferPaymentService transferPaymentService) {
        this.transferPaymentService = transferPaymentService;
    }

    @Transactional(readOnly = true)
    public byte[] generateReceipt(Long orderId, String customerEmail) {

        PurchaseOrder order = transferPaymentService
                .getOrderForReceipt(orderId, customerEmail);

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "A receipt is available only after payment approval."
            );
        }

        if (order.getReceiptNumber() == null
                || order.getReceiptNumber().isBlank()) {
            throw new IllegalStateException(
                    "The approved order does not have a receipt number."
            );
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font regular = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

            PDType1Font bold = new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                float left = 50;
                float y = 785;

                y = writeText(content, bold, 24, left, y,
                        "NaijaMart - Payment Receipt");

                y -= 15;

                content.moveTo(left, y);
                content.lineTo(545, y);
                content.stroke();

                y -= 35;

                y = writeText(content, bold, 12, left, y,
                        "PAYMENT CONFIRMATION");

                y -= 25;

                y = writeText(content, regular, 10, left, y,
                        "Receipt Number: " + order.getReceiptNumber());

                y -= 18;

                y = writeText(content, regular, 10, left, y,
                        "Tracking Number: " + order.getTrackingNumber());

                y -= 18;

                y = writeText(content, regular, 10, left, y,
                        "Customer: " + safe(order.getCustomerName()));

                y -= 18;

                y = writeText(content, regular, 10, left, y,
                        "Email: " + safe(order.getCustomerEmail()));

                y -= 18;

                y = writeText(content, regular, 10, left, y,
                        "Phone: " + safe(order.getCustomerPhone()));

                y -= 18;

                y = writeText(content, regular, 10, left, y,
                        "Delivery Address: " + safe(order.getDeliveryAddress()));

                y -= 30;

                y = writeText(content, bold, 12, left, y,
                        "PURCHASED ITEMS");

                y -= 22;

                y = writeText(content, bold, 9, left, y,
                        "Product");

                y = writeText(content, bold, 9, 300, y,
                        "Qty");

                y = writeText(content, bold, 9, 350, y,
                        "Unit Price");

                y = writeText(content, bold, 9, 455, y,
                        "Subtotal");

                y -= 12;

                for (OrderItem item : order.getItems()) {

                    if (y < 100) {
                        throw new IllegalStateException(
                                "This order has too many items for a single-page receipt."
                        );
                    }

                    String productName = safe(
                            item.getProduct().getName()
                    );

                    y = writeText(content, regular, 9, left, y,
                            productName);

                    y = writeText(content, regular, 9, 300, y,
                            String.valueOf(item.getQuantity()));

                    y = writeText(content, regular, 9, 350, y,
                            "NGN " + money(item.getUnitPrice()));

                    y = writeText(content, regular, 9, 455, y,
                            "NGN " + money(item.getSubtotal()));

                    y -= 18;
                }

                y -= 12;

                content.moveTo(left, y);
                content.lineTo(545, y);
                content.stroke();

                y -= 30;

                y = writeText(content, bold, 13, left, y,
                        "TOTAL PAID: NGN " + money(order.getTotalAmount()));

                y -= 25;

                y = writeText(content, regular, 10, left, y,
                        "Payment Status: PAID - ADMIN APPROVED");

                y -= 18;

                y = writeText(content, regular, 10, left, y,
                        "Payment Reference: "
                                + safe(order.getPaymentReference()));

                y -= 30;

                y = writeText(content, regular, 9, left, y,
                        "Thank you for shopping with NaijaMart.");

                y -= 15;

                writeText(content, regular, 8, left, y,
                        "This receipt confirms an approved bank transfer payment.");
            }

            document.save(output);
            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to generate the PDF receipt.",
                    exception
            );
        }
    }

    private float writeText(
            PDPageContentStream content,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            String text
    ) throws IOException {

        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();

        return y;
    }

    private String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
