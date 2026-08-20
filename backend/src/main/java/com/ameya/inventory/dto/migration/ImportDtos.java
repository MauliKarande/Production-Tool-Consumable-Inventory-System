package com.ameya.inventory.dto.migration;

import java.util.List;

public class ImportDtos {

    public record ImportResult(
            boolean committed,
            String sourceFile,
            int manufacturersCreated,
            int suppliersCreated,
            int itemsCreated,
            int machinesCreated,
            int purchaseRequisitionsCreated,
            int transactionsPosted,
            List<String> warnings,
            List<String> errors
    ) {
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }
}
