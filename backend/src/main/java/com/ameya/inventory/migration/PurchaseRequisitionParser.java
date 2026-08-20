package com.ameya.inventory.migration;

import com.ameya.inventory.entity.Department;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.PrStatus;
import com.ameya.inventory.entity.PurchaseRequisition;
import com.ameya.inventory.entity.PurchaseRequisitionItem;
import com.ameya.inventory.entity.Supplier;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.repository.DepartmentRepository;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.PurchaseRequisitionRepository;
import com.ameya.inventory.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PURCHASE REQUISTION SLIP-*.xls, imported from "Sheet1" - the
 * consolidated master log, not the 17 individual per-slip sheets.
 * Sheet1 already carries requested qty, date, supplier, landed price
 * (with-discount) AND received/shop-floor-use columns in one row, so it
 * is a strict superset of what any one slip sheet has (verified by
 * comparing slip O0017 to Sheet1 rows 1-2, which match exactly). Using
 * it directly avoids the fuzzy (description+date+supplier) matching back
 * to slip sheets that Phase 1 doc F.3 describes - and honors "never
 * auto-guess ambiguous matches" by simply not needing to guess.
 *
 * Rows are grouped into one PurchaseRequisition per (date, supplier) pair,
 * matching the real-world pattern (one slip = one supplier on one day).
 * Every row here represents a completed historical purchase, so each PR
 * is created directly in RECEIVED/CLOSED state (Phase 1 doc: "no
 * retroactive approval history exists to import") rather than replayed
 * through the live DRAFT->...->CLOSED lifecycle service.
 */
@Component
@RequiredArgsConstructor
public class PurchaseRequisitionParser implements ExcelImportParser {

    private final ImportSupport support;
    private final DepartmentRepository departmentRepository;
    private final PurchaseRequisitionRepository prRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionService inventoryTransactionService;

    private record SourceRow(String description, BigDecimal qty, LocalDate date, String supplier,
                              BigDecimal unitPrice, BigDecimal gross, BigDecimal net, BigDecimal discountPct,
                              BigDecimal shopFloorQty) {
    }

    @Override
    public ImportFileType type() {
        return ImportFileType.PURCHASE_REQUISITION;
    }

    @Override
    public void parse(InputStream in, String sourceFileName, ImportContext ctx) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet("Sheet1");
            if (sheet == null) {
                throw new BusinessRuleException("'" + sourceFileName + "' has no 'Sheet1' (the consolidated purchase log) - cannot import.");
            }
            List<SourceRow> rows = readRows(sheet, ctx);
            if (rows.isEmpty()) {
                ctx.warnings.add("No usable rows found in Sheet1 of " + sourceFileName + ".");
                return;
            }

            Map<String, List<SourceRow>> groups = new LinkedHashMap<>();
            for (SourceRow row : rows) {
                String key = row.date() + "|" + (row.supplier() == null ? "" : row.supplier());
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }

            Department production = departmentRepository.findByNameIgnoreCase("PRODUCTION")
                    .orElseThrow(() -> new BusinessRuleException("Expected seeded department 'PRODUCTION' is missing."));
            User legacyUser = support.legacyImportUser();

            for (List<SourceRow> group : groups.values()) {
                createRequisition(group, production, legacyUser, sourceFileName, ctx);
            }
        }
    }

    private void createRequisition(List<SourceRow> group, Department department, User legacyUser, String sourceFileName, ImportContext ctx) {
        Long performedByUserId = legacyUser.getId();
        PurchaseRequisition pr = new PurchaseRequisition();
        pr.setPrNo(generatePrNo());
        pr.setRequestedBy(legacyUser);
        pr.setDepartment(department);
        pr.setStatus(PrStatus.CLOSED);
        pr.setReason("Legacy import from " + sourceFileName + " (Sheet1)");
        pr.setApprovedBy(legacyUser);
        pr.setApprovedAt(Instant.now());
        prRepository.save(pr);
        ctx.purchaseRequisitionsCreated++;

        for (SourceRow row : group) {
            Supplier supplier = support.resolveOrCreateSupplier(row.supplier(), ctx);
            String categoryName = ImportSupport.guessCategoryFromDescription(row.description());
            ItemCategory category = support.requireCategory(categoryName);
            UnitOfMeasure pcs = support.requireUom("PCS");
            Item item = support.resolveOrCreateItem(row.description(), category, pcs, null, supplier,
                    row.description(), null, "LEG", ctx);

            BigDecimal landedUnitCost = row.qty().signum() > 0
                    ? row.net().divide(row.qty(), 2, RoundingMode.HALF_UP)
                    : row.unitPrice();

            var inward = inventoryTransactionService.legacyInward(item.getId(), row.qty(), landedUnitCost, row.date(),
                    "Legacy import: PR receipt from " + (supplier != null ? supplier.getName() : "unknown supplier"), performedByUserId);
            ctx.transactionsPosted++;

            PurchaseRequisitionItem prItem = new PurchaseRequisitionItem();
            prItem.setPr(pr);
            prItem.setItem(item);
            prItem.setQuantity(row.qty());
            prItem.setEstimatedPrice(landedUnitCost);
            prItem.setSupplier(supplier);
            prItem.setReceivedQty(row.qty());
            prItem.setReceivedTxn(transactionRepository.getReferenceById(inward.id()));
            pr.getItems().add(prItem);

            if (row.shopFloorQty().signum() > 0) {
                // Pre-check, not try/catch - a caught exception from a @Transactional call
                // still poisons the outer commit (see ConsumptionSheetParser for the full explanation).
                BigDecimal available = transactionRepository.currentStock(item.getId());
                if (row.shopFloorQty().compareTo(available) > 0) {
                    ctx.warnings.add("'" + row.description() + "': direct-to-floor quantity exceeded just-received stock in the source data - posted anyway.");
                }
                inventoryTransactionService.legacyIssueUnchecked(item.getId(), row.shopFloorQty(), null, row.date(),
                        "Legacy import: direct-to-floor use from PR receipt", performedByUserId);
                ctx.transactionsPosted++;
            }

            BigDecimal expectedGross = row.qty().multiply(row.unitPrice());
            if (expectedGross.subtract(row.gross()).abs().compareTo(BigDecimal.ONE) > 0) {
                ctx.warnings.add("Reconciliation: '" + row.description() + "' qty*unitPrice (" + expectedGross + ") does not match GROSS TOTAL (" + row.gross() + ").");
            }
        }
        prRepository.save(pr);
    }

    private List<SourceRow> readRows(Sheet sheet, ImportContext ctx) {
        int headerRow = -1;
        for (int r = 0; r <= 3; r++) {
            Row row = sheet.getRow(r);
            if (row != null && findColumnByText(row, "DESCRIPTION") >= 0) {
                headerRow = r;
                break;
            }
        }
        if (headerRow < 0) {
            throw new BusinessRuleException("Could not locate the header row (DESCRIPTION column) in Sheet1.");
        }
        Row header = sheet.getRow(headerRow);
        int descCol = findColumnByText(header, "DESCRIPTION");
        int qtyCol = findColumnByText(header, "QTY");
        int dateCol = findColumnByText(header, "DATE");
        int supplierCol = dateCol + 1;
        int unitPriceCol = findColumnByTextAll(header, "AMOUNT", "UNIT");
        int grossCol = findColumnByTextAny(header, "GROOSE", "GROSS");
        int netCol = findColumnByText(header, "WITH DIS");
        int discountCol = findExactColumnByText(header, "DISCOUNT");
        int shopFloorCol = findColumnByText(header, "SHOP");
        if (descCol < 0 || qtyCol < 0 || dateCol < 0 || unitPriceCol < 0 || netCol < 0) {
            throw new BusinessRuleException("Sheet1 header row is missing one or more expected columns (Description/Qty/Date/Amount/Discount).");
        }

        List<SourceRow> rows = new ArrayList<>();
        for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String description = ImportSupport.clean(stringValue(row.getCell(descCol)));
            if (description == null) {
                continue;
            }
            BigDecimal qty = numericValue(row.getCell(qtyCol));
            BigDecimal dateSerial = numericValue(row.getCell(dateCol));
            if (qty.signum() <= 0 || dateSerial.signum() <= 0) {
                ctx.warnings.add("Skipped Sheet1 row for '" + description + "' - missing quantity or date.");
                continue;
            }
            LocalDate date = ImportSupport.excelSerialToLocalDate(dateSerial.doubleValue());
            String supplier = ImportSupport.clean(stringValue(row.getCell(supplierCol)));
            BigDecimal unitPrice = numericValue(row.getCell(unitPriceCol));
            BigDecimal gross = numericValue(row.getCell(grossCol));
            BigDecimal net = numericValue(row.getCell(netCol));
            BigDecimal discountPct = discountCol >= 0 ? numericValue(row.getCell(discountCol)) : BigDecimal.ZERO;
            BigDecimal shopFloor = shopFloorCol >= 0 ? numericValue(row.getCell(shopFloorCol)) : BigDecimal.ZERO;
            if (net.signum() <= 0) {
                net = gross.signum() > 0 ? gross : qty.multiply(unitPrice);
            }
            rows.add(new SourceRow(description, qty, date, supplier, unitPrice, gross, net, discountPct, shopFloor));
        }
        return rows;
    }

    private String generatePrNo() {
        long seq = prRepository.count() + 1;
        String candidate;
        do {
            candidate = "LEG-PR-" + String.format("%04d", seq);
            seq++;
        } while (prRepository.existsByPrNo(candidate));
        return candidate;
    }

    private int findColumnByText(Row row, String contains) {
        for (Cell cell : row) {
            String text = stringValue(cell);
            if (text != null && text.toUpperCase().contains(contains.toUpperCase())) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private int findExactColumnByText(Row row, String exact) {
        for (Cell cell : row) {
            String text = stringValue(cell);
            if (text != null && text.trim().equalsIgnoreCase(exact)) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private int findColumnByTextAll(Row row, String a, String b) {
        for (Cell cell : row) {
            String text = stringValue(cell);
            if (text != null && text.toUpperCase().contains(a.toUpperCase()) && text.toUpperCase().contains(b.toUpperCase())) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private int findColumnByTextAny(Row row, String a, String b) {
        for (Cell cell : row) {
            String text = stringValue(cell);
            if (text != null && (text.toUpperCase().contains(a.toUpperCase()) || text.toUpperCase().contains(b.toUpperCase()))) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private String stringValue(Cell cell) {
        return ImportSupport.stringValue(cell);
    }

    private BigDecimal numericValue(Cell cell) {
        return ImportSupport.numericValue(cell);
    }
}
