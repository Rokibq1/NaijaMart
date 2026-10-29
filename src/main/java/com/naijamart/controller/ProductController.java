
package com.naijamart.controller;

import com.naijamart.entity.Product;
import com.naijamart.service.ProductService;
import com.naijamart.service.StockNotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final StockNotificationService stockNotificationService;

    public ProductController(
            ProductService productService,
            StockNotificationService stockNotificationService) {

        this.productService = productService;
        this.stockNotificationService = stockNotificationService;
    }

    // Public: get all products or search by name
    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String search) {

        if (search != null && !search.isBlank()) {
            return productService.searchProducts(search.trim());
        }

        return productService.getAllActiveProducts();
    }

    // Public: get product details
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    productService.getProductById(id));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Public: request an email when an out-of-stock product returns
    // Example request body: {"email": "customer@example.com"}
    @PostMapping("/{id}/notify-when-available")
    public ResponseEntity<?> requestStockNotification(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {

        try {
            if (request == null || request.get("email") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Please provide your email address"));
            }

            stockNotificationService.registerNotification(
                    id,
                    request.get("email"));

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "You will receive an email when this product is back in stock"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Admin only: create a product
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(
            @RequestBody Product product) {

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(productService.createProduct(product));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Admin only: update a product
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {

        try {
            return ResponseEntity.ok(
                    productService.updateProduct(id, product));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Admin only: restock a product
    // Example request body: {"quantity": 20}
    @PostMapping("/{id}/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restockProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {

        try {
            if (request == null || !request.containsKey("quantity")) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Please provide a restock quantity"));
            }

            Integer quantity = request.get("quantity");

            Product product =
                    productService.restockProduct(id, quantity);

            // Notify customers who registered interest in this product
            int emailsSent =
                    stockNotificationService.notifyWaitingCustomers(product);

            return ResponseEntity.ok(
                    Map.of(
                            "product", product,
                            "stockNotificationEmailsSent", emailsSent));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Admin only: deactivate a product
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {

        try {
            productService.deleteProduct(id);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Product deactivated successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
