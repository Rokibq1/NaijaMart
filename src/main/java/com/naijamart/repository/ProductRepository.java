
package com.naijamart.repository;

import com.naijamart.entity.Product;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find all active products
    List<Product> findByActiveTrue();

    // Search active products by name
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    // Find active products with stock above a specified quantity
    List<Product> findByStockQuantityGreaterThanAndActiveTrue(
            Integer quantity
    );

    // Lock an active product row while checkout checks and updates stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.active = true")
    Optional<Product> findActiveProductForUpdate(@Param("id") Long id);
}
