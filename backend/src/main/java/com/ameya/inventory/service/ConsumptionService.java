package com.ameya.inventory.service;

import com.ameya.inventory.dto.consumption.ConsumptionDtos;
import com.ameya.inventory.entity.Machine;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.InventoryTransactionRepository;
import com.ameya.inventory.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * All figures here are derived on read from the inventory ledger
 * (ISSUE_OUTWARD transactions) - nothing is precomputed or cached, so a
 * report for last March is exactly as reliable as one for today.
 */
@Service
@RequiredArgsConstructor
public class ConsumptionService {

    private final InventoryTransactionRepository transactionRepository;
    private final MachineRepository machineRepository;

    @Transactional(readOnly = true)
    public ConsumptionDtos.MachineConsumptionDetail machineConsumption(Long machineId, LocalDate from, LocalDate to) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> ResourceNotFoundException.of("Machine", machineId));

        List<ConsumptionDtos.ItemConsumption> items = transactionRepository.machineConsumptionByItem(machineId, from, to)
                .stream()
                .map(row -> new ConsumptionDtos.ItemConsumption(
                        row.getItemId(), row.getItemCode(), row.getItemName(),
                        row.getCategoryId(), row.getCategoryName(), row.getQuantity(), row.getValue()))
                .toList();

        BigDecimal totalQty = items.stream().map(ConsumptionDtos.ItemConsumption::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue = items.stream().map(ConsumptionDtos.ItemConsumption::value).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ConsumptionDtos.MachineConsumptionDetail(
                machine.getId(), machine.getMachineCode(), machine.getMachineName(), from, to, totalQty, totalValue, items);
    }

    @Transactional(readOnly = true)
    public List<ConsumptionDtos.MachineConsumption> allMachinesConsumption(LocalDate from, LocalDate to) {
        return transactionRepository.allMachinesConsumption(from, to).stream()
                .map(row -> new ConsumptionDtos.MachineConsumption(
                        row.getMachineId(), row.getMachineCode(), row.getMachineName(), row.getQuantity(), row.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsumptionDtos.CategoryConsumption> categoryConsumption(Long machineId, LocalDate from, LocalDate to) {
        return transactionRepository.categoryConsumption(machineId, from, to).stream()
                .map(row -> new ConsumptionDtos.CategoryConsumption(row.getCategoryId(), row.getCategoryName(), row.getQuantity(), row.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsumptionDtos.ItemConsumption> topConsumedItems(LocalDate from, LocalDate to, String by, int limit) {
        var pageable = PageRequest.of(0, limit);
        var rows = "quantity".equalsIgnoreCase(by)
                ? transactionRepository.topConsumedByQuantity(from, to, pageable)
                : transactionRepository.topConsumedByValue(from, to, pageable);
        return rows.stream()
                .map(row -> new ConsumptionDtos.ItemConsumption(
                        row.getItemId(), row.getItemCode(), row.getItemName(),
                        row.getCategoryId(), row.getCategoryName(), row.getQuantity(), row.getValue()))
                .toList();
    }
}
