package com.ameya.inventory.repository;

import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.entity.PurchaseRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, Long>, JpaSpecificationExecutor<PurchaseRequisition> {
    boolean existsByPrNo(String prNo);

    long countByStatus(PrStatus status);

    @Query("select p.status as status, count(p) as count from PurchaseRequisition p group by p.status")
    List<StatusCount> countGroupedByStatus();

    interface StatusCount {
        PrStatus getStatus();
        Long getCount();
    }
}
