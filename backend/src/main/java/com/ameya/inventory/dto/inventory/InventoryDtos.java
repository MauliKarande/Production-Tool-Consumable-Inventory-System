package com.ameya.inventory.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class InventoryDtos {

    public record IssueRequest(
            @NotNull(message = "Item is required") Long itemId,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotNull(message = "Employee is required") Long employeeId,
            Long machineId,
            String purpose,
            String remark
    ) {
    }

    public record IssueResponse(
            TransactionResponse transaction,
            com.ameya.inventory.dto.assignment.AssignmentDtos.Response assignment,
            BigDecimal remainingStock
    ) {
    }

    public record ReturnRequest(
            @NotNull(message = "Assignment is required") Long assignmentId,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotBlank(message = "Condition is required") String condition,
            String remark
    ) {
    }

    public record ReturnResponse(
            List<TransactionResponse> transactions,
            com.ameya.inventory.dto.assignment.AssignmentDtos.Response assignment
    ) {
    }

    public record OpeningBalanceRequest(
            @NotNull(message = "Item is required") Long itemId,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0", message = "Quantity cannot be negative") BigDecimal quantity,
            @NotNull(message = "Unit cost is required") @DecimalMin(value = "0", message = "Unit cost cannot be negative") BigDecimal unitCost,
            String remark
    ) {
    }

    public record PurchaseInwardRequest(
            @NotNull(message = "Item is required") Long itemId,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotNull(message = "Unit cost is required") @DecimalMin(value = "0", message = "Unit cost cannot be negative") BigDecimal unitCost,
            String remark
    ) {
    }

    public record InwardResponse(
            TransactionResponse transaction,
            BigDecimal newAverageUnitCost,
            BigDecimal newStock
    ) {
    }

    public record AdjustmentRequest(
            @NotNull(message = "Item is required") Long itemId,
            @NotBlank(message = "Direction is required (IN or OUT)") String direction,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotBlank(message = "Reason is required") String reason
    ) {
    }

    public record DamageScrapRequest(
            @NotNull(message = "Item is required") Long itemId,
            @NotBlank(message = "Type is required (DAMAGE or SCRAP)") String type,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotBlank(message = "Reason is required") String reason
    ) {
    }

    public record StockMutationResponse(
            TransactionResponse transaction,
            BigDecimal newStock
    ) {
    }

    public record ReversalRequest(
            @NotNull(message = "Transaction to reverse is required") Long transactionId,
            @NotBlank(message = "Reason is required") String reason
    ) {
    }

    public record CurrentStockResponse(
            Long itemId,
            String itemCode,
            String itemName,
            BigDecimal currentStock,
            BigDecimal safeStock,
            BigDecimal maxStock,
            BigDecimal unitCost,
            BigDecimal inventoryValue,
            String status
    ) {
    }

    public record StockSummaryResponse(
            Long itemId,
            String itemCode,
            String itemName,
            BigDecimal opening,
            BigDecimal purchased,
            BigDecimal issued,
            BigDecimal returned,
            BigDecimal adjustmentIn,
            BigDecimal adjustmentOut,
            BigDecimal damaged,
            BigDecimal scrapped,
            BigDecimal currentStock
    ) {
    }
}
