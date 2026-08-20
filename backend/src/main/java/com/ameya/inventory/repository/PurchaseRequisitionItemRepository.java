package com.ameya.inventory.repository;

import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.entity.PurchaseRequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseRequisitionItemRepository extends JpaRepository<PurchaseRequisitionItem, Long> {

    /** Used by AlertService's PURCHASE_PENDING check: is a low-stock item already covered by an open PR? */
    boolean existsByItem_IdAndPr_StatusIn(Long itemId, List<PrStatus> statuses);

    /** Same item quoted at different prices across suppliers/requisitions - Phase 1 doc Improvement #4. */
    @Query("select i.item.id as itemId, i.item.itemCode as itemCode, i.item.name as itemName, " +
            "i.supplier.id as supplierId, i.supplier.name as supplierName, " +
            "min(i.estimatedPrice) as minPrice, max(i.estimatedPrice) as maxPrice, avg(i.estimatedPrice) as avgPrice, count(i) as timesQuoted " +
            "from PurchaseRequisitionItem i where i.supplier is not null " +
            "group by i.item.id, i.item.itemCode, i.item.name, i.supplier.id, i.supplier.name " +
            "order by i.item.itemCode")
    List<SupplierPriceRow> supplierPriceComparison();

    interface SupplierPriceRow {
        Long getItemId();
        String getItemCode();
        String getItemName();
        Long getSupplierId();
        String getSupplierName();
        BigDecimal getMinPrice();
        BigDecimal getMaxPrice();
        BigDecimal getAvgPrice();
        Long getTimesQuoted();
    }

    /** Total value actually received per supplier - an approximation of spend (Phase 1 doc §H "Purchase" report). */
    @Query("select i.supplier.id as supplierId, i.supplier.name as supplierName, " +
            "sum(i.receivedQty * i.estimatedPrice) as totalSpend, count(i) as lineCount " +
            "from PurchaseRequisitionItem i where i.supplier is not null and i.receivedQty > 0 " +
            "group by i.supplier.id, i.supplier.name order by totalSpend desc")
    List<SupplierSpendRow> supplierSpend();

    interface SupplierSpendRow {
        Long getSupplierId();
        String getSupplierName();
        BigDecimal getTotalSpend();
        Long getLineCount();
    }
}
