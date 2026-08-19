package com.ameya.inventory.dto.machine;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class MachineDtos {

    public record Request(
            @NotBlank(message = "Machine code is required") String machineCode,
            @NotBlank(message = "Machine name is required") String machineName,
            String machineType,
            Long departmentId,
            String location,
            String manufacturer,
            String model,
            String status,
            LocalDate installationDate,
            String remarks,
            boolean active
    ) {
    }

    public record Response(
            Long id,
            String machineCode,
            String machineName,
            String machineType,
            Long departmentId,
            String departmentName,
            String location,
            String manufacturer,
            String model,
            String status,
            LocalDate installationDate,
            String remarks,
            boolean active
    ) {
    }
}
