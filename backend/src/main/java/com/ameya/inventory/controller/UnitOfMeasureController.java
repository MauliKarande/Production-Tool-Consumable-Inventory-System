package com.ameya.inventory.controller;

import com.ameya.inventory.dto.uom.UnitOfMeasureDtos;
import com.ameya.inventory.service.UnitOfMeasureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/units-of-measure")
@RequiredArgsConstructor
public class UnitOfMeasureController {

    private final UnitOfMeasureService service;

    @GetMapping
    public List<UnitOfMeasureDtos.Response> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public UnitOfMeasureDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UnitOfMeasureDtos.Response create(@Valid @RequestBody UnitOfMeasureDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UnitOfMeasureDtos.Response update(@PathVariable Long id, @Valid @RequestBody UnitOfMeasureDtos.Request request) {
        return service.update(id, request);
    }
}
