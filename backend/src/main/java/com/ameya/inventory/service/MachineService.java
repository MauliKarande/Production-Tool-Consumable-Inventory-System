package com.ameya.inventory.service;

import com.ameya.inventory.dto.machine.MachineDtos;
import com.ameya.inventory.entity.Department;
import com.ameya.inventory.entity.Machine;
import com.ameya.inventory.entity.MachineStatus;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.DepartmentRepository;
import com.ameya.inventory.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository repository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public Page<MachineDtos.Response> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MachineDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public MachineDtos.Response create(MachineDtos.Request request) {
        if (repository.existsByMachineCodeIgnoreCase(request.machineCode())) {
            throw new DuplicateResourceException("Machine code '" + request.machineCode() + "' already exists.");
        }
        Machine machine = apply(new Machine(), request);
        return toResponse(repository.save(machine));
    }

    @Transactional
    public MachineDtos.Response update(Long id, MachineDtos.Request request) {
        Machine machine = apply(findOrThrow(id), request);
        return toResponse(repository.save(machine));
    }

    private Machine apply(Machine machine, MachineDtos.Request request) {
        machine.setMachineCode(request.machineCode());
        machine.setMachineName(request.machineName());
        machine.setMachineType(request.machineType());
        machine.setLocation(request.location());
        machine.setManufacturer(request.manufacturer());
        machine.setModel(request.model());
        machine.setInstallationDate(request.installationDate());
        machine.setRemarks(request.remarks());
        machine.setActive(request.active());
        machine.setStatus(request.status() != null ? MachineStatus.valueOf(request.status()) : MachineStatus.ACTIVE);
        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Department", request.departmentId()));
            machine.setDepartment(department);
        } else {
            machine.setDepartment(null);
        }
        return machine;
    }

    private Machine findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Machine", id));
    }

    private MachineDtos.Response toResponse(Machine m) {
        return new MachineDtos.Response(
                m.getId(), m.getMachineCode(), m.getMachineName(), m.getMachineType(),
                m.getDepartment() != null ? m.getDepartment().getId() : null,
                m.getDepartment() != null ? m.getDepartment().getName() : null,
                m.getLocation(), m.getManufacturer(), m.getModel(),
                m.getStatus().name(), m.getInstallationDate(), m.getRemarks(), m.isActive());
    }
}
