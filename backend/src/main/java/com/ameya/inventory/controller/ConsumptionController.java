package com.ameya.inventory.controller;

import com.ameya.inventory.dto.consumption.ConsumptionDtos;
import com.ameya.inventory.service.ConsumptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/consumption")
@RequiredArgsConstructor
public class ConsumptionController {

    private final ConsumptionService service;

    @GetMapping("/machine/{machineId}")
    public ConsumptionDtos.MachineConsumptionDetail machineConsumption(
            @PathVariable Long machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.machineConsumption(machineId, from, to);
    }

    @GetMapping("/machines")
    public List<ConsumptionDtos.MachineConsumption> allMachines(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.allMachinesConsumption(from, to);
    }

    @GetMapping("/category")
    public List<ConsumptionDtos.CategoryConsumption> byCategory(
            @RequestParam(required = false) Long machineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.categoryConsumption(machineId, from, to);
    }

    @GetMapping("/top-items")
    public List<ConsumptionDtos.ItemConsumption> topItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "value") String by,
            @RequestParam(defaultValue = "10") int limit) {
        return service.topConsumedItems(from, to, by, limit);
    }
}
