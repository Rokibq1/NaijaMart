
package com.naijamart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OrderStatusRequest {

    @NotBlank(message = "Order status is required")
    @Pattern(
        regexp = "PENDING|CONFIRMED|PROCESSING|SHIPPED|DELIVERED|CANCELLED",
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "Invalid order status"
    )
    private String status;

    public OrderStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
