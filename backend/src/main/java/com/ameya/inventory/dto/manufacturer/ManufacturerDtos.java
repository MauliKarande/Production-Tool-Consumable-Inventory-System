package com.ameya.inventory.dto.manufacturer;

import jakarta.validation.constraints.NotBlank;

public class ManufacturerDtos {

    public record Request(
            @NotBlank(message = "Name is required") String name,
            boolean active
    ) {
    }

    public record Response(
            Long id,
            String name,
            boolean active
    ) {
    }
}
