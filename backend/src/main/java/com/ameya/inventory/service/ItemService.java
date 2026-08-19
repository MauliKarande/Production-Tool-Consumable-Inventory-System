package com.ameya.inventory.service;

import com.ameya.inventory.dto.item.ItemDtos;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemAttributeValue;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.ItemCategoryAttributeDef;
import com.ameya.inventory.entity.Manufacturer;
import com.ameya.inventory.entity.Supplier;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.ItemAttributeValueRepository;
import com.ameya.inventory.repository.ItemCategoryAttributeDefRepository;
import com.ameya.inventory.repository.ItemCategoryRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.ManufacturerRepository;
import com.ameya.inventory.repository.SupplierRepository;
import com.ameya.inventory.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository categoryRepository;
    private final ItemCategoryAttributeDefRepository attributeDefRepository;
    private final ItemAttributeValueRepository attributeValueRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final SupplierRepository supplierRepository;
    private final UnitOfMeasureRepository uomRepository;

    @Transactional(readOnly = true)
    public Page<ItemDtos.Response> search(String keyword, Long categoryId, Long manufacturerId, Boolean active, Pageable pageable) {
        var spec = ItemSpecifications.and(
                ItemSpecifications.search(keyword),
                ItemSpecifications.categoryId(categoryId),
                ItemSpecifications.manufacturerId(manufacturerId),
                ItemSpecifications.active(active)
        );
        return itemRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ItemDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ItemDtos.Response create(ItemDtos.CreateRequest request) {
        if (itemRepository.existsByItemCodeIgnoreCase(request.itemCode())) {
            throw new DuplicateResourceException("Item code '" + request.itemCode() + "' already exists.");
        }

        Item item = new Item();
        item.setItemCode(request.itemCode());
        item.setName(request.name());
        item.setDescription(request.description());
        item.setCategory(findCategory(request.categoryId()));
        item.setManufacturer(findManufacturerOrNull(request.manufacturerId()));
        item.setPreferredSupplier(findSupplierOrNull(request.preferredSupplierId()));
        item.setUom(findUom(request.uomId()));
        item.setPartNumber(request.partNumber());
        item.setSpecification(request.specification());
        item.setSafeStock(request.safeStock());
        item.setMaxStock(request.maxStock());
        item.setCurrentUnitCost(request.openingUnitCost() != null ? request.openingUnitCost() : BigDecimal.ZERO);
        item.setBarcodeValue(blankToNull(request.barcodeValue()));
        item.setActive(request.active());

        Item saved = itemRepository.save(item);
        applyAttributeValues(saved, request.attributeValues());
        return toResponse(saved);
    }

    @Transactional
    public ItemDtos.Response update(Long id, ItemDtos.UpdateRequest request) {
        Item item = findOrThrow(id);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setCategory(findCategory(request.categoryId()));
        item.setManufacturer(findManufacturerOrNull(request.manufacturerId()));
        item.setPreferredSupplier(findSupplierOrNull(request.preferredSupplierId()));
        item.setUom(findUom(request.uomId()));
        item.setPartNumber(request.partNumber());
        item.setSpecification(request.specification());
        item.setSafeStock(request.safeStock());
        item.setMaxStock(request.maxStock());
        item.setBarcodeValue(blankToNull(request.barcodeValue()));
        item.setActive(request.active());

        Item saved = itemRepository.save(item);
        applyAttributeValues(saved, request.attributeValues());
        return toResponse(saved);
    }

    private void applyAttributeValues(Item item, Map<Long, String> attributeValues) {
        attributeValueRepository.deleteByItem_Id(item.getId());
        if (attributeValues == null || attributeValues.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, String> entry : attributeValues.entrySet()) {
            ItemCategoryAttributeDef def = attributeDefRepository.findById(entry.getKey())
                    .orElseThrow(() -> ResourceNotFoundException.of("Attribute definition", entry.getKey()));
            if (!def.getCategory().getId().equals(item.getCategory().getId())) {
                throw new BusinessRuleException(
                        "Attribute '" + def.getAttributeName() + "' does not belong to category '" + item.getCategory().getName() + "'.");
            }
            ItemAttributeValue value = new ItemAttributeValue();
            value.setItem(item);
            value.setAttributeDef(def);
            value.setValue(entry.getValue());
            attributeValueRepository.save(value);
        }
    }

    private Item findOrThrow(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Item", id));
    }

    private ItemCategory findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Item category", id));
    }

    private Manufacturer findManufacturerOrNull(Long id) {
        if (id == null) {
            return null;
        }
        return manufacturerRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Manufacturer", id));
    }

    private Supplier findSupplierOrNull(Long id) {
        if (id == null) {
            return null;
        }
        return supplierRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    private UnitOfMeasure findUom(Long id) {
        return uomRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Unit of measure", id));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private ItemDtos.Response toResponse(Item item) {
        List<ItemDtos.AttributeValueResponse> attributes = attributeValueRepository.findByItem_Id(item.getId())
                .stream()
                .map(v -> new ItemDtos.AttributeValueResponse(
                        v.getAttributeDef().getId(),
                        v.getAttributeDef().getAttributeName(),
                        v.getAttributeDef().getDataType().name(),
                        v.getValue()))
                .toList();

        return new ItemDtos.Response(
                item.getId(), item.getItemCode(), item.getName(), item.getDescription(),
                item.getCategory().getId(), item.getCategory().getName(),
                item.getManufacturer() != null ? item.getManufacturer().getId() : null,
                item.getManufacturer() != null ? item.getManufacturer().getName() : null,
                item.getPreferredSupplier() != null ? item.getPreferredSupplier().getId() : null,
                item.getPreferredSupplier() != null ? item.getPreferredSupplier().getName() : null,
                item.getUom().getId(), item.getUom().getCode(),
                item.getPartNumber(), item.getSpecification(),
                item.getSafeStock(), item.getMaxStock(), item.getCurrentUnitCost(), item.getCurrency(),
                item.getLegacyDescription(), item.getBarcodeValue(), item.isActive(), attributes);
    }
}
