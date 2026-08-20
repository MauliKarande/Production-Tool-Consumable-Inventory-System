package com.ameya.inventory.repository;

import com.ameya.inventory.entity.Alert;
import com.ameya.inventory.entity.AlertStatus;
import com.ameya.inventory.entity.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByTypeAndStatusIn(AlertType type, List<AlertStatus> statuses);

    long countByStatus(AlertStatus status);

    @Query("select a from Alert a where (:status is null or a.status = :status) " +
            "and (:type is null or a.type = :type) order by a.raisedAt desc")
    Page<Alert> search(@Param("status") AlertStatus status, @Param("type") AlertType type, Pageable pageable);
}
