package com.ameya.inventory.migration;

import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.Machine;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.repository.InventoryTransactionRepository;
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
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JAN-DEC-*.xlsx - one sheet per month, two unrelated tables per sheet
 * (Phase 1 doc F.2): a machine-wise oil table (the only legacy data with
 * real machine attribution) and a general shop-floor-consumables table.
 * Column positions are located dynamically by header text, not hardcoded,
 * since sheet names here are just "1".."5", not the months themselves.
 */
@Component
@RequiredArgsConstructor
public class OilConsumableParser implements ExcelImportParser {

    private static final Pattern MONTH_YEAR = Pattern.compile("(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\D*(\\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Month> MONTHS = Map.ofEntries(
            Map.entry("JAN", Month.JANUARY), Map.entry("FEB", Month.FEBRUARY), Map.entry("MAR", Month.MARCH),
            Map.entry("APR", Month.APRIL), Map.entry("MAY", Month.MAY), Map.entry("JUN", Month.JUNE),
            Map.entry("JUL", Month.JULY), Map.entry("AUG", Month.AUGUST), Map.entry("SEP", Month.SEPTEMBER),
            Map.entry("OCT", Month.OCTOBER), Map.entry("NOV", Month.NOVEMBER), Map.entry("DEC", Month.DECEMBER));

    private final ImportSupport support;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionService inventoryTransactionService;

    private record OilItems(Item cutting, Item hydraulic, Item machine) {
    }

    @Override
    public ImportFileType type() {
        return ImportFileType.OIL_CONSUMABLE;
    }

    @Override
    public void parse(InputStream in, String sourceFileName, ImportContext ctx) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            ItemCategory oils = support.requireCategory("OILS");
            UnitOfMeasure ltr = support.requireUom("LTR");
            OilItems oilItems = new OilItems(
                    support.resolveOrCreateItem("Cutting Oil", oils, ltr, null, null, "Cutting Oil", null, "OIL", ctx),
                    support.resolveOrCreateItem("Hydraulic Oil", oils, ltr, null, null, "Hydraulic Oil", null, "OIL", ctx),
                    support.resolveOrCreateItem("Machine Oil", oils, ltr, null, null, "Machine Oil", null, "OIL", ctx));

            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                parseSheet(sheet, sourceFileName, oilItems, ctx);
            }
        }
    }

    private void parseSheet(Sheet sheet, String sourceFileName, OilItems oilItems, ImportContext ctx) {
        LocalDate month = extractMonth(sheet);
        if (month == null) {
            ctx.warnings.add("Sheet '" + sheet.getSheetName() + "' in " + sourceFileName + ": could not determine month - sheet skipped.");
            return;
        }
        LocalDate lastDay = month.withDayOfMonth(month.lengthOfMonth());

        int[] oilHeader = findHeaderCell(sheet, "M/C NAME");
        if (oilHeader == null) {
            ctx.warnings.add("Sheet '" + sheet.getSheetName() + "': could not locate the machine-oil table header - sheet skipped.");
            return;
        }
        int headerRow = oilHeader[0];
        int machineCol = oilHeader[1];
        int cuttingOilCol = machineCol + 1;
        int hydraulicCol = machineCol + 3;
        int machineOilCol = machineCol + 4;

        int oilRowsImported = 0;
        int r = headerRow + 1;
        for (; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String machineName = row == null ? null : ImportSupport.clean(stringValue(row.getCell(machineCol)));
            if (machineName == null) {
                break;
            }
            Machine machine = support.resolveOrCreateMachine(machineName, ctx);
            BigDecimal cutting = numericValue(row.getCell(cuttingOilCol));
            BigDecimal hydraulic = numericValue(row.getCell(hydraulicCol));
            BigDecimal machineOil = numericValue(row.getCell(machineOilCol));

            postOilIfPresent(oilItems.cutting(), cutting, machine, lastDay, sourceFileName, ctx);
            postOilIfPresent(oilItems.hydraulic(), hydraulic, machine, lastDay, sourceFileName, ctx);
            postOilIfPresent(oilItems.machine(), machineOil, machine, lastDay, sourceFileName, ctx);
            if (cutting.signum() != 0 || hydraulic.signum() != 0 || machineOil.signum() != 0) {
                oilRowsImported++;
            }
        }

        int[] consHeader = findHeaderCell(sheet, "USED THIS MONTH");
        int consRowsImported = 0;
        if (consHeader != null) {
            consRowsImported = parseGeneralConsumables(sheet, consHeader, machineCol, lastDay, sourceFileName, ctx);
        }
        ctx.warnings.add("Sheet '" + sheet.getSheetName() + "' (" + month + "): " + oilRowsImported + " machine oil row(s), " + consRowsImported + " general consumable row(s) imported.");
    }

    private int parseGeneralConsumables(Sheet sheet, int[] consHeader, int itemNameCol, LocalDate lastDay, String sourceFileName, ImportContext ctx) {
        int headerRow = consHeader[0];
        Row headerRowObj = sheet.getRow(headerRow);
        int usedCol = findColumnByText(headerRowObj, "USED");
        int priceCol = findColumnByText(headerRowObj, "PRI");
        int addedCol = findColumnByText(headerRowObj, "ADDED");
        int oldStockCol = findColumnByText(headerRowObj, "OLD STOCK");
        if (usedCol < 0 || priceCol < 0) {
            ctx.warnings.add("Sheet '" + sheet.getSheetName() + "': general consumables table found but columns could not be matched - skipped.");
            return 0;
        }

        ItemCategory other = support.requireCategory("OTHER CONSUMABLES");
        UnitOfMeasure pcs = support.requireUom("PCS");

        int imported = 0;
        for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String name = row == null ? null : ImportSupport.clean(stringValue(row.getCell(itemNameCol)));
            if (name == null) {
                break;
            }
            String upper = name.toUpperCase();
            if (upper.contains("IN LITTERS") || upper.contains("IN LITERS") || upper.contains("CUTTING OIL")
                    || upper.contains("HYDROLIC OIL") || upper.contains("HYDRAULIC OIL") || upper.contains("MACHINE OIL")) {
                ctx.warnings.add("Skipped '" + name + "' in general consumables - already covered by the machine-wise oil table this month.");
                continue;
            }

            BigDecimal used = numericValue(row.getCell(usedCol));
            BigDecimal price = numericValue(row.getCell(priceCol));
            BigDecimal added = addedCol >= 0 ? numericValue(row.getCell(addedCol)) : BigDecimal.ZERO;
            BigDecimal oldStock = oldStockCol >= 0 ? numericValue(row.getCell(oldStockCol)) : BigDecimal.ZERO;

            if (used.signum() == 0 && added.signum() == 0 && oldStock.signum() == 0) {
                continue;
            }

            Item item = support.resolveOrCreateItem(name, other, pcs, null, null, name, null, "CON", ctx);
            boolean alreadyHasHistory = transactionRepository.existsByItem_Id(item.getId());
            if (!alreadyHasHistory) {
                inventoryTransactionService.legacyOpeningBalance(item.getId(), oldStock, price, lastDay.withDayOfMonth(1),
                        "Legacy import: opening stock from " + sourceFileName, ctx.performedByUserId);
                ctx.transactionsPosted++;
            }
            if (added.signum() != 0) {
                inventoryTransactionService.legacyAdjustment(item.getId(), added, true, lastDay,
                        "Legacy import: added in month, " + sourceFileName, ctx.performedByUserId);
                ctx.transactionsPosted++;
            }
            if (used.signum() != 0) {
                // Pre-check, not try/catch - see ConsumptionSheetParser for why a caught
                // exception from a @Transactional call still poisons the outer commit.
                BigDecimal available = transactionRepository.currentStock(item.getId());
                if (used.compareTo(available) > 0) {
                    ctx.warnings.add("'" + name + "' in " + sheet.getSheetName() + ": recorded consumption exceeded recorded stock - posted anyway, ledger will show negative until Admin corrects opening stock.");
                }
                inventoryTransactionService.legacyIssueUnchecked(item.getId(), used, null, lastDay,
                        "Legacy import: monthly consumption, " + sourceFileName, ctx.performedByUserId);
                ctx.transactionsPosted++;
            }
            imported++;
        }
        return imported;
    }

    private void postOilIfPresent(Item item, BigDecimal qty, Machine machine, LocalDate lastDay, String sourceFileName, ImportContext ctx) {
        if (qty.signum() <= 0) {
            return;
        }
        inventoryTransactionService.legacyIssueUnchecked(item.getId(), qty, machine != null ? machine.getId() : null, lastDay,
                "Legacy import: monthly oil consumption, " + sourceFileName, ctx.performedByUserId);
        ctx.transactionsPosted++;
    }

    private LocalDate extractMonth(Sheet sheet) {
        for (int r = 0; r <= 1; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
                // Some sheets store the month label as an actual Excel date cell rather
                // than text (e.g. sheet "5" = May), so a real date always wins first.
                if (cell.getCellType() == CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    var d = cell.getLocalDateTimeCellValue().toLocalDate();
                    return d.withDayOfMonth(1);
                }
                String text = stringValue(cell);
                if (text == null) continue;
                Matcher m = MONTH_YEAR.matcher(text);
                if (m.find()) {
                    Month month = MONTHS.get(m.group(1).toUpperCase());
                    int year = Integer.parseInt(m.group(2));
                    return LocalDate.of(year, month, 1);
                }
            }
        }
        return null;
    }

    private int[] findHeaderCell(Sheet sheet, String containsText) {
        String needle = normalize(containsText);
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
                String text = normalize(stringValue(cell));
                if (text != null && text.contains(needle)) {
                    return new int[]{r, cell.getColumnIndex()};
                }
            }
        }
        return null;
    }

    private int findColumnByText(Row row, String containsText) {
        if (row == null) return -1;
        String needle = normalize(containsText);
        for (Cell cell : row) {
            String text = normalize(stringValue(cell));
            if (text != null && text.contains(needle)) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    /** Source headers have inconsistent internal whitespace (e.g. "USED  THIS MONTH" with two spaces) - collapse before comparing. */
    private String normalize(String text) {
        return text == null ? null : text.toUpperCase().replaceAll("\\s+", " ").trim();
    }

    private String stringValue(Cell cell) {
        return ImportSupport.stringValue(cell);
    }

    private BigDecimal numericValue(Cell cell) {
        return ImportSupport.numericValue(cell);
    }
}
