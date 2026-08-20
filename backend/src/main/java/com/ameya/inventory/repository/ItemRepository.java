package com.ameya.inventory.repository;

import com.ameya.inventory.entity.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    boolean existsByItemCodeIgnoreCase(String itemCode);

    List<Item> findByActiveTrue();

    /** Legacy-import dedup key (Phase 1 doc F.1: description is the only stable identity in the source data). */
    Optional<Item> findByNameIgnoreCase(String name);

    long countByItemCodeStartingWith(String prefix);

    /**
     * Locks the item row for the duration of the caller's transaction so
     * that two concurrent stock-mutating requests for the same item are
     * serialized - the second blocks until the first commits, instead of
     * both reading the same "available" figure and both succeeding.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(Long id);
}
