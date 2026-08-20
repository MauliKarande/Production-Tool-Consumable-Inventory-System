package com.ameya.inventory.repository;

import com.ameya.inventory.entity.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<UnitOfMeasure> findByCodeIgnoreCase(String code);
}
