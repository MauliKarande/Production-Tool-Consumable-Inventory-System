package com.ameya.inventory.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttributeDefDtos {

    public record Request(
            @NotBlank(message = "Attribute name is required") String attributeName,
            @NotNull(message = "Data type is required") String dataType,
            boolean required,
            int displayOrder
    ) {
    }

    public record Response(
            Long id,
            String attributeName,
            String dataType,
            boolean required,
            int displayOrder
    ) {
    }
}
