package com.ameya.inventory.repository;

import com.ameya.inventory.entity.InventoryTransaction;
import com.ameya.inventory.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    boolean existsByReversalOfTxn_Id(Long transactionId);

    boolean existsByItem_Id(Long itemId);

    Page<InventoryTransaction> findByItem_IdOrderByTxnDateDescCreatedAtDesc(Long itemId, Pageable pageable);

    Page<InventoryTransaction> findByMachine_IdOrderByTxnDateDescCreatedAtDesc(Long machineId, Pageable pageable);

    Page<InventoryTransaction> findByEmployee_IdOrderByTxnDateDescCreatedAtDesc(Long employeeId, Pageable pageable);

    @Query("select coalesce(sum(t.quantity), 0) from InventoryTransaction t where t.item.id = :itemId")
    BigDecimal currentStock(@Param("itemId") Long itemId);

    @Query("select t.txnType as type, coalesce(sum(t.quantity), 0) as total " +
            "from InventoryTransaction t where t.item.id = :itemId group by t.txnType")
    List<TxnTypeTotal> sumByType(@Param("itemId") Long itemId);

    interface TxnTypeTotal {
        TransactionType getType();
        BigDecimal getTotal();
    }

    /** Current stock for every item in one query - used by AlertService instead of N+1-ing currentStock(id) per item. */
    @Query("select t.item.id as itemId, coalesce(sum(t.quantity), 0) as stock " +
            "from InventoryTransaction t group by t.item.id")
    List<ItemStockRow> allItemStocks();

    interface ItemStockRow {
        Long getItemId();
        BigDecimal getStock();
    }

    /** Month-bucketed ISSUE_OUTWARD quantity per item, for AlertService's consumption-spike check. */
    @Query("select t.item.id as itemId, function('date_format', t.txnDate, '%Y-%m') as ym, " +
            "sum(abs(t.quantity)) as qty " +
            "from InventoryTransaction t " +
            "where t.txnType = 'ISSUE_OUTWARD' and t.txnDate between :from and :to " +
            "group by t.item.id, function('date_format', t.txnDate, '%Y-%m')")
    List<MonthlyItemQty> monthlyIssuedQtyByItem(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface MonthlyItemQty {
        Long getItemId();
        String getYm();
        BigDecimal getQty();
    }

    /** Items with any ISSUE_OUTWARD on or after a date - used by the dead-stock report to find items with none. */
    @Query("select distinct t.item.id from InventoryTransaction t where t.txnType = 'ISSUE_OUTWARD' and t.txnDate >= :since")
    List<Long> itemIdsIssuedSince(@Param("since") LocalDate since);

    /** Total consumption value over a date range - used by the dashboard's "this month" KPI. */
    @Query("select coalesce(sum(abs(t.quantity) * t.unitCostAtTxn), 0) from InventoryTransaction t " +
            "where t.txnType = 'ISSUE_OUTWARD' and t.txnDate between :from and :to")
    BigDecimal totalConsumptionValue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Item-wise consumption (ISSUE_OUTWARD only) for one machine over a date
     * range - e.g. "what did CNC-1 consume in August". See Phase 1 doc §16.
     */
    @Query("select t.item.id as itemId, t.item.itemCode as itemCode, t.item.name as itemName, " +
            "t.item.category.id as categoryId, t.item.category.name as categoryName, " +
            "sum(abs(t.quantity)) as quantity, sum(abs(t.quantity) * t.unitCostAtTxn) as value " +
            "from InventoryTransaction t " +
            "where t.machine.id = :machineId and t.txnType = 'ISSUE_OUTWARD' and t.txnDate between :from and :to " +
            "group by t.item.id, t.item.itemCode, t.item.name, t.item.category.id, t.item.category.name " +
            "order by value desc")
    List<ItemConsumption> machineConsumptionByItem(@Param("machineId") Long machineId,
                                                     @Param("from") LocalDate from,
                                                     @Param("to") LocalDate to);

    /**
     * Cross-machine comparison over a date range - "which CNC consumes the
     * most" (Phase 1 doc §20 / priority 4).
     */
    @Query("select t.machine.id as machineId, t.machine.machineCode as machineCode, t.machine.machineName as machineName, " +
            "sum(abs(t.quantity)) as quantity, sum(abs(t.quantity) * t.unitCostAtTxn) as value " +
            "from InventoryTransaction t " +
            "where t.machine is not null and t.txnType = 'ISSUE_OUTWARD' and t.txnDate between :from and :to " +
            "group by t.machine.id, t.machine.machineCode, t.machine.machineName " +
            "order by value desc")
    List<MachineConsumption> allMachinesConsumption(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Category-wise consumption, optionally scoped to one machine. */
    @Query("select t.item.category.id as categoryId, t.item.category.name as categoryName, " +
            "sum(abs(t.quantity)) as quantity, sum(abs(t.quantity) * t.unitCostAtTxn) as value " +
            "from InventoryTransaction t " +
            "where t.txnType = 'ISSUE_OUTWARD' and t.txnDate between :from and :to " +
            "and (:machineId is null or t.machine.id = :machineId) " +
            "group by t.item.category.id, t.item.category.name " +
            "order by value desc")
    List<CategoryConsumption> categoryConsumption(@Param("machineId") Long machineId,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to);

    /** Top-consumed items across all machines over a date range, by value. */
    @Query("select t.item.id as itemId, t.item.itemCode as itemCode, t.item.name as itemName, " +
            "t.item.category.id as categoryId, t.item.category.name as categoryName, " +
            "sum(abs(t.quantity)) as quantity, sum(abs(t.quantity) * t.unitCostAtTxn) as value " +
            "from InventoryTransaction t " +
            "where t.txnType = 'ISSUE_OUTWARD' and t.txnDate between :from and :to " +
            "group by t.item.id, t.item.itemCode, t.item.name, t.item.category.id, t.item.category.name " +
            "order by value desc")
    List<ItemConsumption> topConsumedByValue(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    /** Same as above, ranked by quantity instead of value. */
    @Query("select t.item.id as itemId, t.item.itemCode as itemCode, t.item.name as itemName, " +
            "t.item.category.id as categoryId, t.item.category.name as categoryName, " +
            "sum(abs(t.quantity)) as quantity, sum(abs(t.quantity) * t.unitCostAtTxn) as value " +
            "from InventoryTransaction t " +
            "where t.txnType = 'ISSUE_OUTWARD' and t.txnDate between :from and :to " +
            "group by t.item.id, t.item.itemCode, t.item.name, t.item.category.id, t.item.category.name " +
            "order by quantity desc")
    List<ItemConsumption> topConsumedByQuantity(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    interface ItemConsumption {
        Long getItemId();
        String getItemCode();
        String getItemName();
        Long getCategoryId();
        String getCategoryName();
        BigDecimal getQuantity();
        BigDecimal getValue();
    }

    interface MachineConsumption {
        Long getMachineId();
        String getMachineCode();
        String getMachineName();
        BigDecimal getQuantity();
        BigDecimal getValue();
    }

    interface CategoryConsumption {
        Long getCategoryId();
        String getCategoryName();
        BigDecimal getQuantity();
        BigDecimal getValue();
    }
}
