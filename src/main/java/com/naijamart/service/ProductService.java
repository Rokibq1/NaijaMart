
package com.naijamart.service;

import com.naijamart.entity.Product;
import com.naijamart.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Get all active products
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    // Search active products by name
    public List<Product> searchProducts(String name) {
        if (name == null || name.isBlank()) {
            return getAllActiveProducts();
        }

        return productRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(name.trim());
    }

    // Get one active product by ID
    public Product getProductById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found"));
    }

    // Admin: create a product
    @Transactional
    public Product createProduct(Product product) {
        validateProduct(product);

        product.setId(null);

        return productRepository.save(product);
    }

    // Admin: update an existing product
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        Product existing = productRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found"));

        validateProduct(updatedProduct);

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStockQuantity(updatedProduct.getStockQuantity());
        existing.setImageUrl(updatedProduct.getImageUrl());
        existing.setActive(updatedProduct.isActive());

        return productRepository.save(existing);
    }

    // Admin: add stock without replacing the existing quantity
    @Transactional
    public Product restockProduct(Long id, Integer quantity) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Restock quantity must be greater than zero");
        }

        Product product = productRepository
                .findActiveProductForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Active product not found"));

        int currentStock = product.getStockQuantity() == null
                ? 0
                : product.getStockQuantity();

        if (currentStock < 0) {
            throw new IllegalArgumentException(
                    "Existing stock quantity is invalid");
        }

        final int newStock;

        try {
            newStock = Math.addExact(currentStock, quantity);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Restock quantity is too large");
        }

        product.setStockQuantity(newStock);

        return productRepository.save(product);
    }

    // Admin: deactivate a product
    @Transactional
    public void deleteProduct(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found"));

        product.setActive(false);

        productRepository.save(product);
    }

    // Validate product information
    private void validateProduct(Product product) {

        if (product == null) {
            throw new IllegalArgumentException(
                    "Product information is required");
        }

        // Validate product name
        if (product.getName() == null
                || product.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required");
        }

        product.setName(product.getName().trim());

        if (product.getName().length() < 2
                || product.getName().length() > 150) {
            throw new IllegalArgumentException(
                    "Product name must contain between 2 and 150 characters");
        }

        // Validate description
        if (product.getDescription() != null
                && product.getDescription().length() > 2000) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 2000 characters");
        }

        // Validate price
        if (product.getPrice() == null
                || product.getPrice().compareTo(
                        new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException(
                    "Price must be at least 0.01");
        }

        BigDecimal price = product.getPrice();

        if (price.scale() > 2
                || price.precision() - price.scale() > 10) {
            throw new IllegalArgumentException(
                    "Price must have up to 10 whole digits and 2 decimal places");
        }

        // Validate stock quantity
        if (product.getStockQuantity() == null
                || product.getStockQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative");
        }

        // Validate image URL or local image path
        String imageUrl = product.getImageUrl();

        if (imageUrl != null && !imageUrl.isBlank()) {

            imageUrl = imageUrl.trim();

            if (imageUrl.length() > 1000) {
                throw new IllegalArgumentException(
                        "Image URL cannot exceed 1000 characters");
            }

            boolean validLocalPath =
                    imageUrl.matches(
                            "^/images/(?!.*\\.\\.)[A-Za-z0-9._/-]+$");

            boolean validWebUrl =
                    imageUrl.matches(
                            "^https?://[^\\s/]+(?:/[^\\s]*)?$");

            if (!validLocalPath && !validWebUrl) {
                throw new IllegalArgumentException(
                        "Image must be a valid HTTP/HTTPS URL or local /images/ path");
            }

            product.setImageUrl(imageUrl);
        }
    }
}
