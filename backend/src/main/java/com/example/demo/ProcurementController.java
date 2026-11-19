package com.example.demo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/procurements")
public class ProcurementController {
    private final ProcurementService procurement;

    public ProcurementController(ProcurementService procurement) {
        this.procurement = procurement;
    }

    @GetMapping
    public List<PurchaseOrderResponse> getHistory() {
        return procurement.getHistory();
    }

    @PostMapping
    public PurchaseOrderResponse create(@Valid @RequestBody PurchaseOrderRequest request) {
        return procurement.createManualOrder(request);
    }

    @PostMapping("/{purchaseOrderId}/receive")
    public PurchaseOrderResponse receive(@PathVariable long purchaseOrderId) {
        return procurement.receive(purchaseOrderId);
    }
}
