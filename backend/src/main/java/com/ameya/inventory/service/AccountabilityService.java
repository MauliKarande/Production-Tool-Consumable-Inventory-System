package com.ameya.inventory.service;

import com.ameya.inventory.dto.assignment.AssignmentDtos;
import com.ameya.inventory.entity.StockAssignment;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.StockAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountabilityService {

    private final StockAssignmentRepository repository;

    @Transactional(readOnly = true)
    public Page<AssignmentDtos.Response> search(Long employeeId, Long machineId, Long itemId, boolean openOnly, Pageable pageable) {
        var spec = AccountabilitySpecifications.and(
                AccountabilitySpecifications.employeeId(employeeId),
                AccountabilitySpecifications.machineId(machineId),
                AccountabilitySpecifications.itemId(itemId),
                AccountabilitySpecifications.openOnly(openOnly)
        );
        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AssignmentDtos.Response get(Long id) {
        return toResponse(repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Assignment", id)));
    }

    private AssignmentDtos.Response toResponse(StockAssignment a) {
        return new AssignmentDtos.Response(
                a.getId(), a.getItem().getId(), a.getItem().getItemCode(), a.getItem().getName(),
                a.getEmployee().getId(), a.getEmployee().getName(),
                a.getMachine() != null ? a.getMachine().getId() : null,
                a.getMachine() != null ? a.getMachine().getMachineCode() : null,
                a.getAssignedQty(), a.getReturnedQty(), a.getAssignedQty().subtract(a.getReturnedQty()),
                a.getStatus().name(), a.getOpenedAt(), a.getClosedAt());
    }
}
