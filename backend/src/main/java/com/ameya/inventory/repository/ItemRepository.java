package com.ameya.inventory.repository;

import com.ameya.inventory.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    boolean existsByItemCodeIgnoreCase(String itemCode);
}
