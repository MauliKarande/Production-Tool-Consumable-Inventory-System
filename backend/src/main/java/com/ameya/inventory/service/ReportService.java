package com.ameya.inventory.service;

import com.ameya.inventory.dto.report.ReportDtos;
import com.ameya.inventory.entity.AlertStatus;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.repository.AlertRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.PurchaseRequisitionItemRepository;
import com.ameya.inventory.repository.PurchaseRequisitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every report here is a query over the ledger / masters, never a
 * separately maintained table (Phase 1 doc §H) - so a report run today is
 * exactly as reliable as one run a month from now.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ItemRepository itemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final PurchaseRequisitionRepository prRepository;
    private final PurchaseRequisitionItemRepository prItemRepository;

    @Transactional(readOnly = true)
    public List<ReportDtos.StockValuationRow> stockValuation() {
        Map<Long, BigDecimal> stockByItem = stockByItem();
        return itemRepository.findByActiveTrue().stream()
                .map(item -> {
                    BigDecimal stock = stockByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
                    return new ReportDtos.StockValuationRow(
                            item.getId(), item.getItemCode(), item.getName(), item.getCategory().getName(), item.getUom().getCode(),
                            stock, item.getCurrentUnitCost(), stock.multiply(item.getCurrentUnitCost()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.LowStockRow> lowAndOutOfStock() {
        Map<Long, BigDecimal> stockByItem = stockByItem();
        return itemRepository.findByActiveTrue().stream()
                .map(item -> {
                    BigDecimal stock = stockByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
                    boolean out = stock.signum() <= 0;
                    boolean low = !out && item.getSafeStock().signum() > 0 && stock.compareTo(item.getSafeStock()) <= 0;
                    if (!out && !low) {
                        return null;
                    }
                    BigDecimal reorderTarget = item.getMaxStock() != null ? item.getMaxStock() : item.getSafeStock().multiply(BigDecimal.valueOf(2));
                    BigDecimal reorderQty = reorderTarget.subtract(stock).max(BigDecimal.ZERO);
                    return new ReportDtos.LowStockRow(item.getId(), item.getItemCode(), item.getName(), stock,
                            item.getSafeStock(), item.getMaxStock(), reorderQty, out ? "OUT_OF_STOCK" : "LOW_STOCK");
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Items with stock value tied up but no consumption in the last N months (Phase 1 doc Improvement #2). */
    @Transactional(readOnly = true)
    public List<ReportDtos.DeadStockRow> deadStock(int months) {
        LocalDate since = LocalDate.now().minusMonths(months);
        Set<Long> issuedRecently = new HashSet<>(transactionRepository.itemIdsIssuedSince(since));
        Map<Long, BigDecimal> stockByItem = stockByItem();

        return itemRepository.findByActiveTrue().stream()
                .filter(item -> !issuedRecently.contains(item.getId()))
                .map(item -> {
                    BigDecimal stock = stockByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
                    return new ReportDtos.DeadStockRow(item.getId(), item.getItemCode(), item.getName(),
                            item.getCategory().getName(), stock, item.getCurrentUnitCost(), stock.multiply(item.getCurrentUnitCost()));
                })
                .filter(row -> row.value().signum() > 0)
                .sorted((a, b) -> b.value().compareTo(a.value()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.SupplierPriceRow> supplierPriceComparison() {
        return prItemRepository.supplierPriceComparison().stream()
                .map(r -> new ReportDtos.SupplierPriceRow(r.getItemId(), r.getItemCode(), r.getItemName(),
                        r.getSupplierId(), r.getSupplierName(), r.getMinPrice(), r.getMaxPrice(), r.getAvgPrice(), r.getTimesQuoted()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.SupplierSpendRow> supplierSpend() {
        return prItemRepository.supplierSpend().stream()
                .map(r -> new ReportDtos.SupplierSpendRow(r.getSupplierId(), r.getSupplierName(), r.getTotalSpend(), r.getLineCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.PurchasePipelineRow> purchasePipeline() {
        return prRepository.countGroupedByStatus().stream()
                .map(r -> new ReportDtos.PurchasePipelineRow(r.getStatus().name(), r.getCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportDtos.DashboardSummary dashboardSummary() {
        Map<Long, BigDecimal> stockByItem = stockByItem();
        List<Item> items = itemRepository.findByActiveTrue();

        BigDecimal totalValue = BigDecimal.ZERO;
        long low = 0;
        long out = 0;
        for (Item item : items) {
            BigDecimal stock = stockByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
            totalValue = totalValue.add(stock.multiply(item.getCurrentUnitCost()));
            if (stock.signum() <= 0) {
                out++;
            } else if (item.getSafeStock().signum() > 0 && stock.compareTo(item.getSafeStock()) <= 0) {
                low++;
            }
        }

        long openAlerts = alertRepository.countByStatus(AlertStatus.OPEN);
        long openPrs = prRepository.countByStatus(PrStatus.SUBMITTED) + prRepository.countByStatus(PrStatus.APPROVED)
                + prRepository.countByStatus(PrStatus.ORDERED);

        LocalDate to = LocalDate.now();
        LocalDate from = to.withDayOfMonth(1);
        BigDecimal monthConsumption = transactionRepository.totalConsumptionValue(from, to);

        return new ReportDtos.DashboardSummary(items.size(), totalValue, low, out, openAlerts, openPrs, monthConsumption);
    }

    private Map<Long, BigDecimal> stockByItem() {
        Map<Long, BigDecimal> map = new HashMap<>();
        transactionRepository.allItemStocks().forEach(r -> map.put(r.getItemId(), r.getStock()));
        return map;
    }
}
