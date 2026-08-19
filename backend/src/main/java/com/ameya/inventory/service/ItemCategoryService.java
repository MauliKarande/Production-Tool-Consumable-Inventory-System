package com.ameya.inventory.service;

import com.ameya.inventory.dto.category.AttributeDefDtos;
import com.ameya.inventory.dto.category.ItemCategoryDtos;
import com.ameya.inventory.entity.AttributeDataType;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.ItemCategoryAttributeDef;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.ItemCategoryAttributeDefRepository;
import com.ameya.inventory.repository.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemCategoryService {

    private final ItemCategoryRepository repository;
    private final ItemCategoryAttributeDefRepository attributeDefRepository;

    @Transactional(readOnly = true)
    public List<ItemCategoryDtos.Response> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ItemCategoryDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ItemCategoryDtos.Response create(ItemCategoryDtos.Request request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Category '" + request.name() + "' already exists.");
        }
        ItemCategory category = apply(new ItemCategory(), request);
        return toResponse(repository.save(category));
    }

    @Transactional
    public ItemCategoryDtos.Response update(Long id, ItemCategoryDtos.Request request) {
        ItemCategory category = apply(findOrThrow(id), request);
        return toResponse(repository.save(category));
    }

    @Transactional
    public AttributeDefDtos.Response addAttribute(Long categoryId, AttributeDefDtos.Request request) {
        ItemCategory category = findOrThrow(categoryId);
        ItemCategoryAttributeDef def = new ItemCategoryAttributeDef();
        def.setCategory(category);
        def.setAttributeName(request.attributeName());
        def.setDataType(AttributeDataType.valueOf(request.dataType()));
        def.setRequired(request.required());
        def.setDisplayOrder(request.displayOrder());
        return toAttributeResponse(attributeDefRepository.save(def));
    }

    @Transactional
    public void removeAttribute(Long categoryId, Long attributeDefId) {
        ItemCategoryAttributeDef def = attributeDefRepository.findById(attributeDefId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attribute", attributeDefId));
        if (!def.getCategory().getId().equals(categoryId)) {
            throw new ResourceNotFoundException("Attribute " + attributeDefId + " does not belong to category " + categoryId);
        }
        attributeDefRepository.delete(def);
    }

    private ItemCategory apply(ItemCategory category, ItemCategoryDtos.Request request) {
        category.setName(request.name());
        category.setActive(request.active());
        if (request.parentCategoryId() != null) {
            category.setParentCategory(findOrThrow(request.parentCategoryId()));
        } else {
            category.setParentCategory(null);
        }
        return category;
    }

    private ItemCategory findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Item category", id));
    }

    private ItemCategoryDtos.Response toResponse(ItemCategory c) {
        List<AttributeDefDtos.Response> attrs = attributeDefRepository
                .findByCategory_IdOrderByDisplayOrderAsc(c.getId())
                .stream().map(this::toAttributeResponse).toList();
        return new ItemCategoryDtos.Response(
                c.getId(), c.getName(),
                c.getParentCategory() != null ? c.getParentCategory().getId() : null,
                c.getParentCategory() != null ? c.getParentCategory().getName() : null,
                c.isActive(), attrs);
    }

    private AttributeDefDtos.Response toAttributeResponse(ItemCategoryAttributeDef d) {
        return new AttributeDefDtos.Response(d.getId(), d.getAttributeName(), d.getDataType().name(),
                d.isRequired(), d.getDisplayOrder());
    }
}
