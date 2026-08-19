package com.ameya.inventory.controller;

import com.ameya.inventory.dto.category.AttributeDefDtos;
import com.ameya.inventory.dto.category.ItemCategoryDtos;
import com.ameya.inventory.service.ItemCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/item-categories")
@RequiredArgsConstructor
public class ItemCategoryController {

    private final ItemCategoryService service;

    @GetMapping
    public List<ItemCategoryDtos.Response> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ItemCategoryDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ItemCategoryDtos.Response create(@Valid @RequestBody ItemCategoryDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItemCategoryDtos.Response update(@PathVariable Long id, @Valid @RequestBody ItemCategoryDtos.Request request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AttributeDefDtos.Response addAttribute(@PathVariable Long id, @Valid @RequestBody AttributeDefDtos.Request request) {
        return service.addAttribute(id, request);
    }

    @DeleteMapping("/{id}/attributes/{attributeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void removeAttribute(@PathVariable Long id, @PathVariable Long attributeId) {
        service.removeAttribute(id, attributeId);
    }
}
