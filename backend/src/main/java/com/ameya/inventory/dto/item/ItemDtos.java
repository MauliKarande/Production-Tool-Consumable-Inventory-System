package com.ameya.inventory.dto.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ItemDtos {

    public record CreateRequest(
            @NotBlank(message = "Item code is required") String itemCode,
            @NotBlank(message = "Name is required") String name,
            String description,
            @NotNull(message = "Category is required") Long categoryId,
            Long manufacturerId,
            Long preferredSupplierId,
            @NotNull(message = "Unit of measure is required") Long uomId,
            String partNumber,
            String specification,
            @NotNull(message = "Safe stock is required") @DecimalMin(value = "0", message = "Safe stock cannot be negative") BigDecimal safeStock,
            BigDecimal maxStock,
            @DecimalMin(value = "0", message = "Unit cost cannot be negative") BigDecimal openingUnitCost,
            String barcodeValue,
            boolean active,
            Map<Long, String> attributeValues
    ) {
    }

    public record UpdateRequest(
            @NotBlank(message = "Name is required") String name,
            String description,
            @NotNull(message = "Category is required") Long categoryId,
            Long manufacturerId,
            Long preferredSupplierId,
            @NotNull(message = "Unit of measure is required") Long uomId,
            String partNumber,
            String specification,
            @NotNull(message = "Safe stock is required") @DecimalMin(value = "0", message = "Safe stock cannot be negative") BigDecimal safeStock,
            BigDecimal maxStock,
            String barcodeValue,
            boolean active,
            Map<Long, String> attributeValues
    ) {
    }

    public record AttributeValueResponse(
            Long attributeDefId,
            String attributeName,
            String dataType,
            String value
    ) {
    }

    public record Response(
            Long id,
            String itemCode,
            String name,
            String description,
            Long categoryId,
            String categoryName,
            Long manufacturerId,
            String manufacturerName,
            Long preferredSupplierId,
            String supplierName,
            Long uomId,
            String uomCode,
            String partNumber,
            String specification,
            BigDecimal safeStock,
            BigDecimal maxStock,
            BigDecimal currentUnitCost,
            String currency,
            String legacyDescription,
            String barcodeValue,
            boolean active,
            List<AttributeValueResponse> attributes
    ) {
    }
}
