
package com.naijamart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CheckoutRequest {

    // Customer name validation
    @NotBlank(message = "Customer name is required")
    @Size(
        min = 2,
        max = 80,
        message = "Customer name must contain between 2 and 80 characters"
    )
    @Pattern(
        regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M}' -]*$",
        message = "Customer name contains invalid characters"
    )
    private String customerName;

    // Customer email validation
    @NotBlank(message = "Customer email is required")
    @Email(message = "Please enter a valid customer email")
    @Size(
        max = 254,
        message = "Email cannot exceed 254 characters"
    )
    private String customerEmail;

    // Nigerian phone number validation
    @NotBlank(message = "Customer phone number is required")
    @Pattern(
        regexp = "^(?:0[789][01]\\d{8}|\\+?234[789][01]\\d{8})$",
        message = "Enter a valid Nigerian mobile number"
    )
    @Size(
        max = 20,
        message = "Phone number cannot exceed 20 characters"
    )
    private String customerPhone;

    // Delivery address validation
    @NotBlank(message = "Delivery address is required")
    @Size(
        min = 10,
        max = 500,
        message = "Delivery address must contain between 10 and 500 characters"
    )
    private String deliveryAddress;

    // Cart validation
    @NotEmpty(message = "Your cart cannot be empty")
    @Size(
        max = 100,
        message = "You cannot order more than 100 different items"
    )
    @Valid
    private List<OrderLineRequest> items;

    public static class OrderLineRequest {

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be greater than zero")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public List<OrderLineRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderLineRequest> items) {
        this.items = items;
    }
}
