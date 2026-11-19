package com.example.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProcurementService {
    private final PurchaseOrderRepository purchaseOrders;
    private final ProductRepository products;

    public ProcurementService(PurchaseOrderRepository purchaseOrders, ProductRepository products) {
        this.purchaseOrders = purchaseOrders;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getHistory() {
        return purchaseOrders.findAllByOrderByOrderedAtDesc().stream()
                .map(PurchaseOrderResponse::from).toList();
    }

    @Transactional
    public PurchaseOrderResponse createManualOrder(PurchaseOrderRequest request) {
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません"));
        PurchaseOrder order = new PurchaseOrder(product, request.supplierName().strip(),
                request.expectedDeliveryDate(), request.unitCost(), request.quantity(), false);
        return PurchaseOrderResponse.from(purchaseOrders.save(order));
    }

    /** 基準在庫以下で、未入荷の発注がない商品に自動発注を作成する。 */
    @Transactional
    public void createAutomaticOrderIfNeeded(Product product) {
        if (!product.isAutoOrderEnabled() || product.getStockQuantity() > product.getReorderThreshold()) return;
        if (purchaseOrders.existsByProductIdAndStatus(product.getId(), PurchaseOrderStatus.ORDERED)) return;
        validateAutomaticOrderSettings(product);
        purchaseOrders.save(new PurchaseOrder(product, product.getDefaultSupplierName().strip(),
                LocalDate.now().plusDays(product.getDefaultLeadTimeDays()),
                product.getDefaultOrderUnitCost(), product.getReorderQuantity(), true));
    }

    @Transactional
    public PurchaseOrderResponse receive(long purchaseOrderId) {
        PurchaseOrder order = purchaseOrders.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "発注履歴が見つかりません"));
        if (order.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "すでに入荷済みです");
        }
        Product product = products.findByIdForOrder(order.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません"));
        product.setStockQuantity(Math.addExact(product.getStockQuantity(), order.getQuantity()));
        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());
        products.save(product);
        PurchaseOrder saved = purchaseOrders.save(order);
        createAutomaticOrderIfNeeded(product);
        return PurchaseOrderResponse.from(saved);
    }

    void validateAutomaticOrderSettings(Product product) {
        if (!product.isAutoOrderEnabled()) return;
        if (product.getDefaultSupplierName() == null || product.getDefaultSupplierName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "自動発注の発注先を入力してください");
        }
        if (product.getReorderQuantity() < 1 || product.getDefaultLeadTimeDays() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "自動発注の発注数と納期日数は1以上にしてください");
        }
    }
}
