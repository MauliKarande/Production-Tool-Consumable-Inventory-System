package com.ameya.inventory.controller;

import com.ameya.inventory.dto.assignment.AssignmentDtos;
import com.ameya.inventory.service.AccountabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accountability")
@RequiredArgsConstructor
public class AccountabilityController {

    private final AccountabilityService service;

    @GetMapping
    public Page<AssignmentDtos.Response> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "true") boolean openOnly,
            Pageable pageable) {
        return service.search(employeeId, machineId, itemId, openOnly, pageable);
    }

    @GetMapping("/{id}")
    public AssignmentDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }
}
