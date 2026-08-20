package com.ameya.inventory.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class PurchaseRequisitionDtos {

    public record LineRequest(
            @NotNull(message = "Item is required") Long itemId,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotNull(message = "Estimated price is required") @DecimalMin(value = "0", message = "Estimated price cannot be negative") BigDecimal estimatedPrice,
            Long supplierId
    ) {
    }

    public record CreateRequest(
            Long departmentId,
            String priority,
            String reason,
            @NotEmpty(message = "At least one line item is required") List<@Valid LineRequest> items
    ) {
    }

    public record RejectRequest(
            @NotBlank(message = "A reason is required to reject a requisition") String reason
    ) {
    }

    public record ReceiveLineRequest(
            @NotNull(message = "PR line is required") Long prItemId,
            @NotNull(message = "Received quantity is required") @DecimalMin(value = "0.001", message = "Received quantity must be greater than zero") BigDecimal receivedQty,
            @NotNull(message = "Unit cost is required") @DecimalMin(value = "0", message = "Unit cost cannot be negative") BigDecimal unitCost,
            boolean directToFloor,
            Long machineId
    ) {
    }

    public record ReceiveRequest(
            @NotEmpty(message = "At least one line item is required") List<@Valid ReceiveLineRequest> lines
    ) {
    }

    public record LineResponse(
            Long id,
            Long itemId,
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal estimatedPrice,
            Long supplierId,
            String supplierName,
            BigDecimal receivedQty,
            Long receivedTxnId
    ) {
    }

    public record Response(
            Long id,
            String prNo,
            Long requestedByUserId,
            String requestedByUsername,
            Long departmentId,
            String departmentName,
            String status,
            String priority,
            String reason,
            Long approvedByUserId,
            String approvedByUsername,
            Instant approvedAt,
            Instant createdAt,
            Instant updatedAt,
            List<LineResponse> items
    ) {
    }
}
