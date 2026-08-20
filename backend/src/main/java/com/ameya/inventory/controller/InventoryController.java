package com.ameya.inventory.controller;

import com.ameya.inventory.dto.inventory.InventoryDtos;
import com.ameya.inventory.dto.inventory.TransactionResponse;
import com.ameya.inventory.security.UserPrincipal;
import com.ameya.inventory.service.InventoryTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryTransactionService service;

    @PostMapping("/opening-balance")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryDtos.StockMutationResponse openingBalance(@Valid @RequestBody InventoryDtos.OpeningBalanceRequest request,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        return service.openingBalance(request, principal.getId());
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public InventoryDtos.IssueResponse issue(@Valid @RequestBody InventoryDtos.IssueRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return service.issue(request, principal.getId());
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public InventoryDtos.ReturnResponse returnStock(@Valid @RequestBody InventoryDtos.ReturnRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return service.returnStock(request, principal.getId());
    }

    @PostMapping("/inward")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public InventoryDtos.InwardResponse inward(@Valid @RequestBody InventoryDtos.PurchaseInwardRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return service.purchaseInward(request, principal.getId());
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryDtos.StockMutationResponse adjust(@Valid @RequestBody InventoryDtos.AdjustmentRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return service.adjust(request, principal.getId());
    }

    @PostMapping("/damage-scrap")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryDtos.StockMutationResponse damageOrScrap(@Valid @RequestBody InventoryDtos.DamageScrapRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return service.damageOrScrap(request, principal.getId());
    }

    @PostMapping("/reversal")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryDtos.StockMutationResponse reverse(@Valid @RequestBody InventoryDtos.ReversalRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return service.reverse(request, principal.getId());
    }

    @GetMapping("/current-stock/{itemId}")
    public InventoryDtos.CurrentStockResponse currentStock(@PathVariable Long itemId) {
        return service.getCurrentStock(itemId);
    }

    @GetMapping("/history/machine/{machineId}")
    public Page<TransactionResponse> historyByMachine(@PathVariable Long machineId, Pageable pageable) {
        return service.getHistoryByMachine(machineId, pageable);
    }

    @GetMapping("/history/employee/{employeeId}")
    public Page<TransactionResponse> historyByEmployee(@PathVariable Long employeeId, Pageable pageable) {
        return service.getHistoryByEmployee(employeeId, pageable);
    }
}
