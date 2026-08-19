package com.ameya.inventory.dto.employee;

import jakarta.validation.constraints.NotBlank;

public class EmployeeDtos {

    public record Request(
            @NotBlank(message = "Employee code is required") String employeeCode,
            @NotBlank(message = "Name is required") String name,
            Long departmentId,
            String designation,
            String contact,
            boolean active
    ) {
    }

    public record Response(
            Long id,
            String employeeCode,
            String name,
            Long departmentId,
            String departmentName,
            String designation,
            String contact,
            boolean active
    ) {
    }
}
