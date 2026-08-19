package com.ameya.inventory.repository;

import com.ameya.inventory.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}
