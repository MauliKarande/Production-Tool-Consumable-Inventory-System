package com.ameya.inventory.repository;

import com.ameya.inventory.entity.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    boolean existsByCodeIgnoreCase(String code);
}
