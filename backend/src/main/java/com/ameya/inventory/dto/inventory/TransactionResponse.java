package com.ameya.inventory.dto.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        String txnNo,
        Long itemId,
        String itemCode,
        String itemName,
        String txnType,
        BigDecimal quantity,
        BigDecimal unitCostAtTxn,
        BigDecimal totalValue,
        Long machineId,
        String machineCode,
        Long employeeId,
        String employeeName,
        String performedByUsername,
        String purpose,
        String remark,
        String itemCondition,
        Long reversalOfTxnId,
        String source,
        LocalDate txnDate,
        Instant createdAt
) {
}
