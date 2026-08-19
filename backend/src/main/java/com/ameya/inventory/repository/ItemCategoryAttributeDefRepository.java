package com.ameya.inventory.repository;

import com.ameya.inventory.entity.ItemCategoryAttributeDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCategoryAttributeDefRepository extends JpaRepository<ItemCategoryAttributeDef, Long> {
    List<ItemCategoryAttributeDef> findByCategory_IdOrderByDisplayOrderAsc(Long categoryId);
}
