package com.ameya.inventory.migration;

import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.Manufacturer;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.exception.BusinessRuleException;
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
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * INSERT / DRILLS / TAPS tabs of INSERT CONSUMPTION SHEET *.xlsx (Phase 1
 * doc F.1). The three tabs are NOT byte-for-byte identical (DRILLS/TAPS
 * lack Make/Category columns, and every tab's header sits one row lower
 * than INSERT's) but share one structural invariant verified against the
 * real file: every column of interest sits at a FIXED OFFSET from the
 * "INSERT No." column, regardless of which row/column that column itself
 * lands on. Locating "INSERT No." dynamically and working in offsets from
 * it is what makes one parser correct for all three tabs.
 */
@Component
@RequiredArgsConstructor
public class ConsumptionSheetParser implements ExcelImportParser {

    private static final Pattern MONTH_YEAR = Pattern.compile("(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\D*(\\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Month> MONTHS = Map.ofEntries(
            Map.entry("JAN", Month.JANUARY), Map.entry("FEB", Month.FEBRUARY), Map.entry("MAR", Month.MARCH),
            Map.entry("APR", Month.APRIL), Map.entry("MAY", Month.MAY), Map.entry("JUN", Month.JUNE),
            Map.entry("JUL", Month.JULY), Map.entry("AUG", Month.AUGUST), Map.entry("SEP", Month.SEPTEMBER),
            Map.entry("OCT", Month.OCTOBER), Map.entry("NOV", Month.NOVEMBER), Map.entry("DEC", Month.DECEMBER));

    private static final int OFFSET_NEW_ADD = 6;
    private static final int OFFSET_OPENING = 7;
    private static final int OFFSET_BAL = 8;
    private static final int OFFSET_USED = 9;
    private static final int OFFSET_DAILY_START = 10;
    private static final int DAILY_SPAN = 31;
    private static final int OFFSET_TOTAL_USED = OFFSET_DAILY_START + DAILY_SPAN;
    private static final int OFFSET_PRICE = OFFSET_TOTAL_USED + 1;

    private final ImportSupport support;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryTransactionService inventoryTransactionService;

    @Override
    public ImportFileType type() {
        return ImportFileType.CONSUMPTION_SHEET;
    }

    @Override
    public void parse(InputStream in, String sourceFileName, ImportContext ctx) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                String tabName = sheet.getSheetName().trim().toUpperCase();
                String categoryName = switch (tabName) {
                    case "INSERT" -> "INSERTS";
                    case "DRILLS" -> "DRILLS";
                    case "TAPS" -> "TAPS";
                    default -> null;
                };
                if (categoryName == null) {
                    ctx.warnings.add("Skipped unrecognized tab '" + sheet.getSheetName() + "' in " + sourceFileName + ".");
                    continue;
                }
                parseTab(sheet, categoryName, sourceFileName, ctx);
            }
        }
    }

    private void parseTab(Sheet sheet, String categoryName, String sourceFileName, ImportContext ctx) {
        LocalDate monthStart = extractMonth(sheet, sourceFileName);
        int[] found = locateDescriptionColumn(sheet);
        if (found == null) {
            ctx.warnings.add("Could not locate 'INSERT No.' header in tab '" + sheet.getSheetName() + "' - tab skipped.");
            return;
        }
        int headerRow = found[0];
        int descCol = found[1];
        int dataStartRow = headerRow + 2;

        ItemCategory category = support.requireCategory(categoryName);
        UnitOfMeasure pcs = support.requireUom("PCS");
        String codePrefix = switch (categoryName) {
            case "INSERTS" -> "INS";
            case "DRILLS" -> "DRL";
            default -> "TAP";
        };
        boolean hasMakeCategory = categoryName.equals("INSERTS");

        int lastRow = sheet.getLastRowNum();
        int imported = 0;
        for (int r = dataStartRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String description = ImportSupport.clean(stringValue(row.getCell(descCol)));
            if (description == null) {
                continue;
            }

            BigDecimal opening = numericValue(row.getCell(descCol + OFFSET_OPENING));
            BigDecimal bal = numericValue(row.getCell(descCol + OFFSET_BAL));
            BigDecimal used = numericValue(row.getCell(descCol + OFFSET_USED));
            BigDecimal price = numericValue(row.getCell(descCol + OFFSET_PRICE));

            Map<Integer, BigDecimal> daily = new LinkedHashMap<>();
            BigDecimal dailySum = BigDecimal.ZERO;
            for (int day = 1; day <= DAILY_SPAN; day++) {
                BigDecimal v = numericValue(row.getCell(descCol + OFFSET_DAILY_START + day - 1));
                if (v.signum() != 0) {
                    daily.put(day, v);
                    dailySum = dailySum.add(v);
                }
            }

            boolean hasMovement = opening.signum() != 0 || bal.signum() != 0 || used.signum() != 0 || !daily.isEmpty();
            if (!hasMovement) {
                continue;
            }

            String make = hasMakeCategory ? ImportSupport.clean(stringValue(row.getCell(descCol + 1))) : null;
            String applicationAttr = hasMakeCategory ? ImportSupport.clean(stringValue(row.getCell(descCol + 2))) : null;

            Manufacturer manufacturer = support.resolveOrCreateManufacturer(make, ctx);
            Item item = support.resolveOrCreateItem(description, category, pcs, manufacturer, null,
                    description, applicationAttr, codePrefix, ctx);

            boolean alreadyHasHistory = transactionRepository.existsByItem_Id(item.getId());
            if (!alreadyHasHistory) {
                inventoryTransactionService.legacyOpeningBalance(item.getId(), opening, price, monthStart,
                        "Legacy import: opening stock from " + sourceFileName, ctx.performedByUserId);
                ctx.transactionsPosted++;
            } else {
                ctx.warnings.add("Item '" + description + "' already had ledger history - skipped opening balance, kept daily consumption.");
            }

            for (var entry : daily.entrySet()) {
                LocalDate txnDate = safeDate(monthStart, entry.getKey());
                if (txnDate == null) {
                    continue;
                }
                // Pre-check rather than try/catch around a @Transactional call: an exception
                // thrown from inside legacyIssue() marks the whole outer commit rollback-only
                // even if caught here, since it shares this method's transaction (REQUIRED
                // propagation) - see ExcelImportService for the full explanation.
                BigDecimal available = transactionRepository.currentStock(item.getId());
                if (entry.getValue().compareTo(available) > 0) {
                    ctx.warnings.add("'" + description + "' day " + entry.getKey() + ": recorded consumption exceeded recorded stock (opening=" + opening +
                            ") - posted anyway, ledger will show negative until Admin corrects opening stock.");
                }
                inventoryTransactionService.legacyIssueUnchecked(item.getId(), entry.getValue(), null, txnDate,
                        "Legacy import: daily consumption, " + sourceFileName, ctx.performedByUserId);
                ctx.transactionsPosted++;
            }

            if (dailySum.compareTo(used) != 0) {
                ctx.warnings.add("Reconciliation: '" + description + "' daily total (" + dailySum + ") does not match USED STOCK (" + used + ") in " + sheet.getSheetName() + ".");
            }
            if (opening.subtract(used).compareTo(bal) != 0) {
                ctx.warnings.add("Reconciliation: '" + description + "' OPENING-USED (" + opening.subtract(used) + ") does not match BAL. STOCK (" + bal + ") in " + sheet.getSheetName() + ".");
            }
            imported++;
        }
        ctx.warnings.add("Tab '" + sheet.getSheetName() + "': " + imported + " active item(s) imported.");
    }

    private LocalDate safeDate(LocalDate monthStart, int day) {
        int lengthOfMonth = monthStart.lengthOfMonth();
        if (day > lengthOfMonth) {
            return null;
        }
        return monthStart.withDayOfMonth(day);
    }

    private LocalDate extractMonth(Sheet sheet, String sourceFileName) {
        for (int r = 0; r <= 2; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
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
        Matcher m = MONTH_YEAR.matcher(sourceFileName);
        if (m.find()) {
            Month month = MONTHS.get(m.group(1).toUpperCase());
            int year = Integer.parseInt(m.group(2));
            return LocalDate.of(year, month, 1);
        }
        throw new BusinessRuleException("Could not determine the month/year for '" + sourceFileName + "' - expected e.g. 'JUN-2026' in the title or filename.");
    }

    /** Finds the row/column of the "INSERT No." header cell, searching the first handful of rows. */
    private int[] locateDescriptionColumn(Sheet sheet) {
        for (int r = 0; r <= 4; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
                String text = stringValue(cell);
                if (text != null && text.replaceAll("\\s+", " ").trim().equalsIgnoreCase("INSERT No.")) {
                    return new int[]{r, cell.getColumnIndex()};
                }
            }
        }
        return null;
    }

    private String stringValue(Cell cell) {
        return ImportSupport.stringValue(cell);
    }

    private BigDecimal numericValue(Cell cell) {
        return ImportSupport.numericValue(cell);
    }
}
