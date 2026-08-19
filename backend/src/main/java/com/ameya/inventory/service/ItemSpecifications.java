package com.ameya.inventory.service;

import com.ameya.inventory.entity.Item;
import org.springframework.data.jpa.domain.Specification;

final class ItemSpecifications {

    private ItemSpecifications() {
    }

    static Specification<Item> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("itemCode")), pattern),
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("legacyDescription")), pattern)
        );
    }

    static Specification<Item> categoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Item> manufacturerId(Long manufacturerId) {
        if (manufacturerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("manufacturer").get("id"), manufacturerId);
    }

    static Specification<Item> active(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    static Specification<Item> and(Specification<Item>... specs) {
        Specification<Item> result = Specification.where(null);
        for (Specification<Item> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
