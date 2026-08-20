package com.ameya.inventory.repository;

import com.ameya.inventory.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Manufacturer> findByNameIgnoreCase(String name);
}
