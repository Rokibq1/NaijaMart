
package com.naijamart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(
        min = 2,
        max = 150,
        message = "Product name must contain between 2 and 150 characters"
    )
    @Column(nullable = false, length = 150)
    private String name;

    @Size(
        max = 2000,
        message = "Description cannot exceed 2000 characters"
    )
    @Column(length = 2000)
    private String description;

    @NotNull(message = "Product price is required")
    @DecimalMin(
        value = "0.01",
        message = "Product price must be greater than zero"
    )
    @Digits(
        integer = 10,
        fraction = 2,
        message = "Price must have up to 10 whole digits and 2 decimal places"
    )
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Size(
        max = 1000,
        message = "Image URL cannot exceed 1000 characters"
    )
    @Pattern(
        regexp = "^(?:$|/images/(?!.*\\.\\.)[A-Za-z0-9._/-]+|https?://[^\\s/]+(?:/[^\\s]*)?)$",
        message = "Image must be a valid HTTP/HTTPS URL or a local /images/ path"
    )
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Product() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
