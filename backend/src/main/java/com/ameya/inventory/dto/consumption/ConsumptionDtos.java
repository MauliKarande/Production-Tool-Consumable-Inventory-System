package com.ameya.inventory.dto.consumption;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ConsumptionDtos {

    public record ItemConsumption(
            Long itemId,
            String itemCode,
            String itemName,
            Long categoryId,
            String categoryName,
            BigDecimal quantity,
            BigDecimal value
    ) {
    }

    public record MachineConsumption(
            Long machineId,
            String machineCode,
            String machineName,
            BigDecimal quantity,
            BigDecimal value
    ) {
    }

    public record CategoryConsumption(
            Long categoryId,
            String categoryName,
            BigDecimal quantity,
            BigDecimal value
    ) {
    }

    public record MachineConsumptionDetail(
            Long machineId,
            String machineCode,
            String machineName,
            LocalDate from,
            LocalDate to,
            BigDecimal totalQuantity,
            BigDecimal totalValue,
            List<ItemConsumption> items
    ) {
    }
}
