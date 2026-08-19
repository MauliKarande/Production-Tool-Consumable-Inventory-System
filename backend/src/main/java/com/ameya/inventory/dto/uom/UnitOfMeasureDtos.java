package com.ameya.inventory.dto.uom;

import jakarta.validation.constraints.NotBlank;

public class UnitOfMeasureDtos {

    public record Request(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name,
            boolean active
    ) {
    }

    public record Response(
            Long id,
            String code,
            String name,
            boolean active
    ) {
    }
}
