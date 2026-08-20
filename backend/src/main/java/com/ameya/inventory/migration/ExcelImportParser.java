package com.ameya.inventory.migration;

import java.io.InputStream;

public interface ExcelImportParser {
    ImportFileType type();

    /** Parses the workbook and performs all resolves/writes directly (the caller controls commit vs rollback via the transaction boundary). */
    void parse(InputStream in, String sourceFileName, ImportContext ctx) throws Exception;
}
