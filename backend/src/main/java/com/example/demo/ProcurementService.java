package com.example.demo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProcurementService {
    private static final LocalDate MYSQL_MAX_DATE = LocalDate.of(9999, 12, 31);
    private final PurchaseOrderRepository purchaseOrders;
    private final ProductRepository products;
    private final Clock clock;

    public ProcurementService(PurchaseOrderRepository purchaseOrders, ProductRepository products, Clock clock) {
        this.purchaseOrders = purchaseOrders;
        this.products = products;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getHistory() {
        return purchaseOrders.findAllByOrderByOrderedAtDesc().stream()
                .map(PurchaseOrderResponse::from).toList();
    }

    @Transactional
    public PurchaseOrderResponse createManualOrder(PurchaseOrderRequest request) {
        Product product = products.findByIdForOrder(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません"));
        if (request.expectedDeliveryDate().isBefore(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "納期は今日以降の日付を指定してください");
        }
        PurchaseOrder order = new PurchaseOrder(product, request.supplierName().strip(),
                request.expectedDeliveryDate(), request.unitCost(), request.quantity(), false,
                LocalDateTime.now(clock));
        return PurchaseOrderResponse.from(purchaseOrders.save(order));
    }

    /** 基準在庫以下で、未入荷の発注がない商品に自動発注を作成する。 */
    @Transactional
    public void createAutomaticOrderIfNeeded(long productId) {
        Product product = products.findByIdForOrder(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません"));
        if (!product.isAutoOrderEnabled() || product.getStockQuantity() > product.getReorderThreshold()) return;
        if (!purchaseOrders.findByProductIdAndStatusForUpdate(
                product.getId(), PurchaseOrderStatus.ORDERED).isEmpty()) return;
        validateAutomaticOrderSettings(product);
        purchaseOrders.save(new PurchaseOrder(product, product.getDefaultSupplierName().strip(),
                calculateExpectedDeliveryDate(product.getDefaultLeadTimeDays()),
                product.getDefaultOrderUnitCost(), product.getReorderQuantity(), true,
                LocalDateTime.now(clock)));
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
        order.setReceivedAt(LocalDateTime.now(clock));
        products.save(product);
        PurchaseOrder saved = purchaseOrders.save(order);
        createAutomaticOrderIfNeeded(product.getId());
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
        calculateExpectedDeliveryDate(product.getDefaultLeadTimeDays());
    }

    private LocalDate calculateExpectedDeliveryDate(int leadTimeDays) {
        LocalDate expectedDeliveryDate = LocalDate.now(clock).plusDays(leadTimeDays);
        if (expectedDeliveryDate.isAfter(MYSQL_MAX_DATE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "納期日数が保存可能な範囲を超えています");
        }
        return expectedDeliveryDate;
    }
}
