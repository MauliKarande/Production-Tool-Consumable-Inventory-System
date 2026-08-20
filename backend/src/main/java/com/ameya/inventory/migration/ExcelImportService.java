package com.ameya.inventory.migration;

import com.ameya.inventory.dto.migration.ImportDtos;
import com.ameya.inventory.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Preview and commit run the exact same parser code, inside a real
 * transaction - preview always rolls it back at the end (Spring's
 * TransactionTemplate + setRollbackOnly), so what you see in preview is
 * guaranteed to be what commit would actually do, not a separate
 * approximation that could drift out of sync with it.
 */
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportService.class);

    private final PlatformTransactionManager transactionManager;
    private final ConsumptionSheetParser consumptionSheetParser;
    private final OilConsumableParser oilConsumableParser;
    private final PurchaseRequisitionParser purchaseRequisitionParser;

    public ImportDtos.ImportResult preview(ImportFileType type, byte[] fileBytes, String fileName, Long userId) {
        return run(type, fileBytes, fileName, userId, true);
    }

    public ImportDtos.ImportResult commit(ImportFileType type, byte[] fileBytes, String fileName, Long userId) {
        return run(type, fileBytes, fileName, userId, false);
    }

    private ImportDtos.ImportResult run(ImportFileType type, byte[] fileBytes, String fileName, Long userId, boolean previewOnly) {
        ExcelImportParser parser = resolveParser(type);
        ImportContext ctx = new ImportContext(userId);
        List<String> errors = new ArrayList<>();

        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        try {
            tt.execute(status -> {
                try {
                    parser.parse(new ByteArrayInputStream(fileBytes), fileName, ctx);
                } catch (BusinessRuleException e) {
                    errors.add(e.getMessage());
                } catch (Exception e) {
                    log.error("Import parse error for {} ({})", fileName, type, e);
                    errors.add("Unexpected error while parsing: " + e.getMessage());
                }
                if (previewOnly || !errors.isEmpty()) {
                    status.setRollbackOnly();
                }
                return null;
            });
        } catch (Exception e) {
            // Can happen on commit-phase flush (e.g. a constraint violation only detected when
            // Hibernate finally flushes pending inserts) - outside the inner try/catch above,
            // so it must be caught here or the caller gets a raw, undiagnosable 500.
            log.error("Import transaction failed for {} ({})", fileName, type, e);
            errors.add("Unexpected error while saving: " + e.getMessage());
        }

        return new ImportDtos.ImportResult(!previewOnly && errors.isEmpty(), fileName,
                ctx.manufacturersCreated, ctx.suppliersCreated, ctx.itemsCreated, ctx.machinesCreated,
                ctx.purchaseRequisitionsCreated, ctx.transactionsPosted, ctx.warnings, errors);
    }

    private ExcelImportParser resolveParser(ImportFileType type) {
        return switch (type) {
            case CONSUMPTION_SHEET -> consumptionSheetParser;
            case OIL_CONSUMABLE -> oilConsumableParser;
            case PURCHASE_REQUISITION -> purchaseRequisitionParser;
        };
    }
}
