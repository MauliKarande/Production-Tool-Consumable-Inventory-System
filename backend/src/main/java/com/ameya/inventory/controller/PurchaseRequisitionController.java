package com.ameya.inventory.controller;

import com.ameya.inventory.dto.purchase.PurchaseRequisitionDtos;
import com.ameya.inventory.security.UserPrincipal;
import com.ameya.inventory.service.PurchaseRequisitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-requisitions")
@RequiredArgsConstructor
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService service;

    @GetMapping
    public Page<PurchaseRequisitionDtos.Response> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long departmentId,
            Pageable pageable) {
        return service.search(status, priority, departmentId, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseRequisitionDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public PurchaseRequisitionDtos.Response create(@Valid @RequestBody PurchaseRequisitionDtos.CreateRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return service.create(request, principal.getId());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public PurchaseRequisitionDtos.Response submit(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.submit(id, principal.getId());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public PurchaseRequisitionDtos.Response approve(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.approve(id, principal.getId());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public PurchaseRequisitionDtos.Response reject(@PathVariable Long id,
                                                     @Valid @RequestBody PurchaseRequisitionDtos.RejectRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return service.reject(id, request, principal.getId());
    }

    @PostMapping("/{id}/mark-ordered")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public PurchaseRequisitionDtos.Response markOrdered(@PathVariable Long id) {
        return service.markOrdered(id);
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public PurchaseRequisitionDtos.Response receive(@PathVariable Long id,
                                                       @Valid @RequestBody PurchaseRequisitionDtos.ReceiveRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return service.receiveGoods(id, request, principal.getId());
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public PurchaseRequisitionDtos.Response close(@PathVariable Long id) {
        return service.close(id);
    }
}
