package com.ameya.inventory.migration;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import com.ameya.inventory.entity.Item;
import com.ameya.inventory.entity.ItemCategory;
import com.ameya.inventory.entity.Machine;
import com.ameya.inventory.entity.MachineStatus;
import com.ameya.inventory.entity.Manufacturer;
import com.ameya.inventory.entity.Supplier;
import com.ameya.inventory.entity.UnitOfMeasure;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.BusinessRuleException;
import com.ameya.inventory.repository.ItemCategoryRepository;
import com.ameya.inventory.repository.ItemRepository;
import com.ameya.inventory.repository.MachineRepository;
import com.ameya.inventory.repository.ManufacturerRepository;
import com.ameya.inventory.repository.SupplierRepository;
import com.ameya.inventory.repository.UnitOfMeasureRepository;
import com.ameya.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resolve-or-create helpers shared by every parser, plus the Excel-serial
 * date conversion each of the three source files needs explicitly
 * (Phase 1 doc A.3: "must be handled explicitly during migration, not left
 * to spreadsheet formatting"). Dedup is always an exact case-insensitive
 * name match - per the doc's own instruction, spelling-variant merging
 * (ISCAR/ISKAR/ISKSR) is an Admin review decision, never auto-guessed here.
 */
@Component
@RequiredArgsConstructor
public class ImportSupport {

    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    private final ManufacturerRepository manufacturerRepository;
    private final SupplierRepository supplierRepository;
    private final ItemCategoryRepository categoryRepository;
    private final UnitOfMeasureRepository uomRepository;
    private final MachineRepository machineRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public Manufacturer resolveOrCreateManufacturer(String rawName, ImportContext ctx) {
        String name = clean(rawName);
        if (name == null) {
            return null;
        }
        return manufacturerRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Manufacturer m = new Manufacturer();
            m.setName(name);
            m.setActive(true);
            Manufacturer saved = manufacturerRepository.save(m);
            ctx.manufacturersCreated++;
            return saved;
        });
    }

    public Supplier resolveOrCreateSupplier(String rawName, ImportContext ctx) {
        String name = clean(rawName);
        if (name == null) {
            return null;
        }
        return supplierRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Supplier s = new Supplier();
            s.setName(name);
            s.setActive(true);
            Supplier saved = supplierRepository.save(s);
            ctx.suppliersCreated++;
            return saved;
        });
    }

    public ItemCategory requireCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new BusinessRuleException("Expected seeded category '" + name + "' is missing - check V2 migration."));
    }

    public UnitOfMeasure requireUom(String code) {
        return uomRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessRuleException("Expected seeded UOM '" + code + "' is missing - check V2 migration."));
    }

    public Machine resolveOrCreateMachine(String rawCode, ImportContext ctx) {
        String code = clean(rawCode);
        if (code == null) {
            return null;
        }
        return machineRepository.findByMachineCodeIgnoreCase(code).orElseGet(() -> {
            Machine m = new Machine();
            m.setMachineCode(code);
            m.setMachineName(code);
            m.setStatus(MachineStatus.ACTIVE);
            m.setActive(true);
            Machine saved = machineRepository.save(m);
            ctx.machinesCreated++;
            ctx.warnings.add("Created machine '" + code + "' - not in the seeded machine list, found in source data.");
            return saved;
        });
    }

    /** Dedup key = item name (trimmed, case-insensitive) - the only stable identity the legacy data has. */
    public Item resolveOrCreateItem(String rawName, ItemCategory category, UnitOfMeasure uom,
                                     Manufacturer manufacturer, Supplier preferredSupplier,
                                     String legacyDescription, String specification, String codePrefix, ImportContext ctx) {
        String name = clean(rawName);
        if (name == null) {
            return null;
        }
        return itemRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Item item = new Item();
            item.setItemCode(generateItemCode(codePrefix));
            item.setName(name);
            item.setCategory(category);
            item.setManufacturer(manufacturer);
            item.setPreferredSupplier(preferredSupplier);
            item.setUom(uom);
            item.setSpecification(specification);
            item.setLegacyDescription(legacyDescription != null ? legacyDescription : name);
            item.setSafeStock(BigDecimal.ZERO);
            item.setCurrentUnitCost(BigDecimal.ZERO);
            item.setActive(true);
            Item saved = itemRepository.save(item);
            ctx.itemsCreated++;
            return saved;
        });
    }

    public String generateItemCode(String prefix) {
        long existing = itemRepository.countByItemCodeStartingWith(prefix + "-");
        String candidate;
        long seq = existing + 1;
        do {
            candidate = prefix + "-" + String.format("%04d", seq);
            seq++;
        } while (itemRepository.existsByItemCodeIgnoreCase(candidate));
        return candidate;
    }

    public User legacyImportUser() {
        return userRepository.findByUsername("legacy.import")
                .orElseThrow(() -> new BusinessRuleException("Legacy import system user is missing - check V3 migration."));
    }

    /** Excel stores dates as a day count from 1899-12-30 (with the historical leap-year-1900 quirk baked in). */
    public static LocalDate excelSerialToLocalDate(double serial) {
        return EXCEL_EPOCH.plusDays((long) serial);
    }

    /** "MILLING" -> item category heuristic used only for the Purchase file's un-categorized free-text items. */
    public static String guessCategoryFromDescription(String description) {
        String d = description.toUpperCase();
        if (d.contains("INSERT")) return "INSERTS";
        if (d.contains("DRILL")) return "DRILLS";
        if (d.contains("TAP")) return "TAPS";
        if (d.contains("END MILL") || d.contains("ENDMILL")) return "END MILLS";
        return "OTHER CONSUMABLES";
    }

    /**
     * These source workbooks compute OPENING STOCK / BAL. STOCK / USED
     * STOCK / TOTAL columns with formulas (=SUM(...) etc.), not literal
     * numbers - a naive check for CellType.NUMERIC/STRING silently reads
     * every formula cell as blank/zero. Both helpers fall back to the
     * cell's cached (last-saved) formula result, which is what every
     * other cell reader (Excel itself, openpyxl's data_only mode) shows.
     */
    public static String stringValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.STRING) return cell.getStringCellValue();
        if (type == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        return null;
    }

    public static BigDecimal numericValue(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        if (type == CellType.STRING) return cleanNumber(cell.getStringCellValue());
        return BigDecimal.ZERO;
    }

    public static String clean(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.toString().trim();
        return s.isEmpty() ? null : s;
    }

    public static BigDecimal cleanNumber(Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        if (raw instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
