package com.ameya.inventory.repository;

import com.ameya.inventory.entity.StockAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockAssignmentRepository extends JpaRepository<StockAssignment, Long>, JpaSpecificationExecutor<StockAssignment> {
}
