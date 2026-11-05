
package com.naijamart.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    // FIND ORDER BY TRACKING NUMBER
    Optional<PurchaseOrder> findByTrackingNumber(
            String trackingNumber);

    // FIND ORDER BY TRACKING NUMBER AND CUSTOMER EMAIL
    Optional<PurchaseOrder> findByTrackingNumberAndCustomerEmail(
            String trackingNumber,
            String customerEmail);

    // ADMIN: GET ALL ORDERS WITH ITEMS AND PRODUCTS
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    // FIND ORDER WITH ITEMS BY ORDER ID
    @Query("""
            select distinct o
            from PurchaseOrder o
            left join fetch o.items i
            left join fetch i.product
            where o.id = :id
            """)
    Optional<PurchaseOrder> findOrderWithItemsById(
            @Param("id") Long id);
}
