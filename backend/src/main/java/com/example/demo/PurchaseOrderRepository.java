package com.example.demo;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByOrderedAtDesc();

    @Query("select purchaseOrder.product.id from PurchaseOrder purchaseOrder "
            + "where purchaseOrder.id = :purchaseOrderId")
    Optional<Long> findProductIdById(@Param("purchaseOrderId") Long purchaseOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from PurchaseOrder purchaseOrder "
            + "where purchaseOrder.product.id = :productId and purchaseOrder.status = :status")
    List<PurchaseOrder> findByProductIdAndStatusForUpdate(
            @Param("productId") Long productId, @Param("status") PurchaseOrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from PurchaseOrder purchaseOrder where purchaseOrder.id = :purchaseOrderId")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("purchaseOrderId") Long purchaseOrderId);
}
