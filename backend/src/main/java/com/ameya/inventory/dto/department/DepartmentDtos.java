package com.ameya.inventory.dto.department;

import jakarta.validation.constraints.NotBlank;

public class DepartmentDtos {

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
