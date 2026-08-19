package com.ameya.inventory.service;

import com.ameya.inventory.dto.department.DepartmentDtos;
import com.ameya.inventory.entity.Department;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repository;

    @Transactional(readOnly = true)
    public List<DepartmentDtos.Response> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public DepartmentDtos.Response create(DepartmentDtos.Request request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Department '" + request.name() + "' already exists.");
        }
        Department department = new Department();
        department.setName(request.name());
        department.setActive(request.active());
        return toResponse(repository.save(department));
    }

    @Transactional
    public DepartmentDtos.Response update(Long id, DepartmentDtos.Request request) {
        Department department = findOrThrow(id);
        department.setName(request.name());
        department.setActive(request.active());
        return toResponse(repository.save(department));
    }

    private Department findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    private DepartmentDtos.Response toResponse(Department d) {
        return new DepartmentDtos.Response(d.getId(), d.getName(), d.isActive());
    }
}
