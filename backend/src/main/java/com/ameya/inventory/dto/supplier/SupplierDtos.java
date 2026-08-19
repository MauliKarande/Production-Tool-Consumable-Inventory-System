package com.ameya.inventory.dto.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SupplierDtos {

    public record Request(
            @NotBlank(message = "Name is required") String name,
            String contactPerson,
            String phone,
            @Email(message = "Email must be valid") String email,
            String address,
            String gstNumber,
            boolean active
    ) {
    }

    public record Response(
            Long id,
            String name,
            String contactPerson,
            String phone,
            String email,
            String address,
            String gstNumber,
            boolean active
    ) {
    }
}
