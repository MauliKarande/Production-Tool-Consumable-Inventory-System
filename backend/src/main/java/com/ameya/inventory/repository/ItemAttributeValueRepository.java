package com.ameya.inventory.repository;

import com.ameya.inventory.entity.ItemAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemAttributeValueRepository extends JpaRepository<ItemAttributeValue, Long> {
    List<ItemAttributeValue> findByItem_Id(Long itemId);

    void deleteByItem_Id(Long itemId);
}
