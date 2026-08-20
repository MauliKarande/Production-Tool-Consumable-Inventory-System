package com.ameya.inventory.controller;

import com.ameya.inventory.dto.alert.AlertDtos;
import com.ameya.inventory.security.UserPrincipal;
import com.ameya.inventory.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService service;

    @GetMapping
    public Page<AlertDtos.Response> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String type,
                                          Pageable pageable) {
        return service.list(status, type, pageable);
    }

    @GetMapping("/open-count")
    public long openCount() {
        return service.openCount();
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public AlertDtos.Response acknowledge(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.acknowledge(id, principal.getId());
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    public AlertDtos.Response resolve(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.resolve(id, principal.getId());
    }

    @PostMapping("/recompute")
    @PreAuthorize("hasRole('ADMIN')")
    public AlertDtos.RecomputeResult recompute() {
        return service.recomputeAll();
    }
}
