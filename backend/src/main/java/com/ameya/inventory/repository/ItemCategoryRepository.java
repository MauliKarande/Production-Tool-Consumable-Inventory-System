package com.ameya.inventory.repository;

import com.ameya.inventory.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<ItemCategory> findByNameIgnoreCase(String name);
}
