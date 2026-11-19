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
    boolean existsByProductIdAndStatus(Long productId, PurchaseOrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from PurchaseOrder purchaseOrder where purchaseOrder.id = :purchaseOrderId")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("purchaseOrderId") Long purchaseOrderId);
}
