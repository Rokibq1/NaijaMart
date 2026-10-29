
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
        return productRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(name);
    }

    // Get one active product by ID
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));
    }

    // Admin: create a product
    @Transactional
    public Product createProduct(Product product) {
        validateProduct(product);

        if (product.getStockQuantity() == null
                || product.getStockQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative");
        }

        product.setId(null);

        return productRepository.save(product);
    }

    // Admin: update an existing product
    @Transactional
    public Product updateProduct(
            Long id,
            Product updatedProduct) {

        Product existing = productRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        validateProduct(updatedProduct);

        if (updatedProduct.getStockQuantity() == null
                || updatedProduct.getStockQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative");
        }

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStockQuantity(
                updatedProduct.getStockQuantity());
        existing.setImageUrl(updatedProduct.getImageUrl());
        existing.setActive(updatedProduct.isActive());

        return productRepository.save(existing);
    }

    // Admin: add stock without replacing the existing quantity
    @Transactional
    public Product restockProduct(
            Long id,
            Integer quantity) {

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
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found"));

        product.setActive(false);

        productRepository.save(product);
    }

    // Validate product information
    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException(
                    "Product information is required");
        }

        if (product.getName() == null
                || product.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required");
        }

        if (product.getPrice() == null
                || product.getPrice().compareTo(
                        BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Price must be greater than zero");
        }
    }
}
