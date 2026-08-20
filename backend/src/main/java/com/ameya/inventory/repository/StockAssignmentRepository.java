package com.ameya.inventory.repository;

import com.ameya.inventory.entity.AssignmentStatus;
import com.ameya.inventory.entity.StockAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface StockAssignmentRepository extends JpaRepository<StockAssignment, Long>, JpaSpecificationExecutor<StockAssignment> {

    /** Used by AlertService's PENDING_RETURN check - assignments still open past a configurable age threshold. */
    List<StockAssignment> findByStatusInAndOpenedAtBefore(List<AssignmentStatus> statuses, Instant threshold);
}
