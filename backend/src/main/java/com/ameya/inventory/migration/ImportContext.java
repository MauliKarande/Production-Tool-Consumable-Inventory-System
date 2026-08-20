package com.ameya.inventory.migration;

import java.util.ArrayList;
import java.util.List;

/** Mutable running total shared by one parse pass - not a DTO, converted to ImportDtos.ImportResult when done. */
public class ImportContext {
    public int manufacturersCreated = 0;
    public int suppliersCreated = 0;
    public int itemsCreated = 0;
    public int machinesCreated = 0;
    public int purchaseRequisitionsCreated = 0;
    public int transactionsPosted = 0;
    public final List<String> warnings = new ArrayList<>();
    public final Long performedByUserId;

    public ImportContext(Long performedByUserId) {
        this.performedByUserId = performedByUserId;
    }
}
