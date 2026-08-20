package com.ameya.inventory.dto.report;

import java.math.BigDecimal;

public class ReportDtos {

    public record StockValuationRow(
            Long itemId, String itemCode, String itemName, String categoryName, String uomCode,
            BigDecimal currentStock, BigDecimal unitCost, BigDecimal value
    ) {
    }

    public record LowStockRow(
            Long itemId, String itemCode, String itemName, BigDecimal currentStock,
            BigDecimal safeStock, BigDecimal maxStock, BigDecimal reorderQty, String status
    ) {
    }

    public record DeadStockRow(
            Long itemId, String itemCode, String itemName, String categoryName,
            BigDecimal currentStock, BigDecimal unitCost, BigDecimal value
    ) {
    }

    public record SupplierPriceRow(
            Long itemId, String itemCode, String itemName, Long supplierId, String supplierName,
            BigDecimal minPrice, BigDecimal maxPrice, BigDecimal avgPrice, long timesQuoted
    ) {
    }

    public record SupplierSpendRow(
            Long supplierId, String supplierName, BigDecimal totalSpend, long lineCount
    ) {
    }

    public record PurchasePipelineRow(String status, long count) {
    }

    public record DashboardSummary(
            long itemCount,
            BigDecimal totalStockValue,
            long lowStockCount,
            long outOfStockCount,
            long openAlertCount,
            long openPrCount,
            BigDecimal thisMonthConsumptionValue
    ) {
    }
}
