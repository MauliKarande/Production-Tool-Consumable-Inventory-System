package com.ameya.inventory.service;

import com.ameya.inventory.dto.employee.EmployeeDtos;
import com.ameya.inventory.entity.Department;
import com.ameya.inventory.entity.Employee;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.DepartmentRepository;
import com.ameya.inventory.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public Page<EmployeeDtos.Response> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public EmployeeDtos.Response create(EmployeeDtos.Request request) {
        if (repository.existsByEmployeeCodeIgnoreCase(request.employeeCode())) {
            throw new DuplicateResourceException("Employee code '" + request.employeeCode() + "' already exists.");
        }
        Employee employee = apply(new Employee(), request);
        return toResponse(repository.save(employee));
    }

    @Transactional
    public EmployeeDtos.Response update(Long id, EmployeeDtos.Request request) {
        Employee employee = apply(findOrThrow(id), request);
        return toResponse(repository.save(employee));
    }

    private Employee apply(Employee employee, EmployeeDtos.Request request) {
        employee.setEmployeeCode(request.employeeCode());
        employee.setName(request.name());
        employee.setDesignation(request.designation());
        employee.setContact(request.contact());
        employee.setActive(request.active());
        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Department", request.departmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }
        return employee;
    }

    private Employee findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }

    private EmployeeDtos.Response toResponse(Employee e) {
        return new EmployeeDtos.Response(
                e.getId(), e.getEmployeeCode(), e.getName(),
                e.getDepartment() != null ? e.getDepartment().getId() : null,
                e.getDepartment() != null ? e.getDepartment().getName() : null,
                e.getDesignation(), e.getContact(), e.isActive());
    }
}
