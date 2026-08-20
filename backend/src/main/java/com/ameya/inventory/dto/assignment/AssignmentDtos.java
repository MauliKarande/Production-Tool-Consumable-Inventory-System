package com.ameya.inventory.dto.assignment;

import java.math.BigDecimal;
import java.time.Instant;

public class AssignmentDtos {

    public record Response(
            Long id,
            Long itemId,
            String itemCode,
            String itemName,
            Long employeeId,
            String employeeName,
            Long machineId,
            String machineCode,
            BigDecimal assignedQty,
            BigDecimal returnedQty,
            BigDecimal remainingQty,
            String status,
            Instant openedAt,
            Instant closedAt
    ) {
    }
}
