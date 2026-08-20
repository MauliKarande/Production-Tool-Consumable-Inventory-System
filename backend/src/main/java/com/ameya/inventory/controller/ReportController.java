package com.ameya.inventory.controller;

import com.ameya.inventory.dto.report.ReportDtos;
import com.ameya.inventory.service.ReportService;
import com.ameya.inventory.util.TableExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @GetMapping("/dashboard-summary")
    public ReportDtos.DashboardSummary dashboardSummary() {
        return service.dashboardSummary();
    }

    @GetMapping("/stock-valuation")
    public Object stockValuation(@RequestParam(defaultValue = "json") String format) {
        return export("Stock Valuation", format, service.stockValuation(),
                List.of("Item Code", "Item Name", "Category", "UOM", "Current Stock", "Unit Cost", "Value"),
                r -> List.of(r.itemCode(), r.itemName(), r.categoryName(), r.uomCode(), r.currentStock(), r.unitCost(), r.value()));
    }

    @GetMapping("/low-stock")
    public Object lowStock(@RequestParam(defaultValue = "json") String format) {
        return export("Low / Out of Stock", format, service.lowAndOutOfStock(),
                List.of("Item Code", "Item Name", "Current Stock", "Safe Stock", "Max Stock", "Reorder Qty", "Status"),
                r -> List.of(r.itemCode(), r.itemName(), r.currentStock(), r.safeStock(),
                        r.maxStock() != null ? r.maxStock() : "", r.reorderQty(), r.status()));
    }

    @GetMapping("/dead-stock")
    public Object deadStock(@RequestParam(defaultValue = "json") String format, @RequestParam(defaultValue = "3") int months) {
        return export("Dead Stock (no consumption in " + months + " months)", format, service.deadStock(months),
                List.of("Item Code", "Item Name", "Category", "Current Stock", "Unit Cost", "Value"),
                r -> List.of(r.itemCode(), r.itemName(), r.categoryName(), r.currentStock(), r.unitCost(), r.value()));
    }

    @GetMapping("/supplier-price-comparison")
    public Object supplierPriceComparison(@RequestParam(defaultValue = "json") String format) {
        return export("Supplier Price Comparison", format, service.supplierPriceComparison(),
                List.of("Item Code", "Item Name", "Supplier", "Min Price", "Max Price", "Avg Price", "Times Quoted"),
                r -> List.of(r.itemCode(), r.itemName(), r.supplierName(), r.minPrice(), r.maxPrice(), r.avgPrice(), r.timesQuoted()));
    }

    @GetMapping("/supplier-spend")
    public Object supplierSpend(@RequestParam(defaultValue = "json") String format) {
        return export("Supplier Spend", format, service.supplierSpend(),
                List.of("Supplier", "Total Spend", "Line Count"),
                r -> List.of(r.supplierName(), r.totalSpend(), r.lineCount()));
    }

    @GetMapping("/purchase-pipeline")
    public Object purchasePipeline(@RequestParam(defaultValue = "json") String format) {
        return export("Purchase Pipeline", format, service.purchasePipeline(),
                List.of("Status", "Count"),
                r -> List.of(r.status(), r.count()));
    }

    private <T> Object export(String title, String format, List<T> data, List<String> headers, Function<T, List<Object>> rowMapper) {
        if ("json".equalsIgnoreCase(format)) {
            return data;
        }
        List<List<Object>> rows = data.stream().map(rowMapper).toList();
        String filename = title.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        if ("xlsx".equalsIgnoreCase(format)) {
            byte[] bytes = TableExporter.toXlsx(title, headers, rows);
            return fileResponse(bytes, filename + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] bytes = TableExporter.toPdf(title, headers, rows);
            return fileResponse(bytes, filename + ".pdf", "application/pdf");
        }
        return data;
    }

    private ResponseEntity<byte[]> fileResponse(byte[] bytes, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
