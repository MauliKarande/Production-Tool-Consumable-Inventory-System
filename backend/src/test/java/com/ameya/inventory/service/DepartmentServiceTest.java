package com.ameya.inventory.service;

import com.ameya.inventory.dto.department.DepartmentDtos;
import com.ameya.inventory.entity.Department;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository repository;

    @InjectMocks
    private DepartmentService service;

    private Department existing;

    @BeforeEach
    void setUp() {
        existing = new Department();
        existing.setId(1L);
        existing.setName("PRODUCTION");
        existing.setActive(true);
    }

    @Test
    void create_savesNewDepartment_whenNameIsUnique() {
        DepartmentDtos.Request request = new DepartmentDtos.Request("STORE", true);
        when(repository.existsByNameIgnoreCase("STORE")).thenReturn(false);
        when(repository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(2L);
            return d;
        });

        DepartmentDtos.Response response = service.create(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("STORE");
        assertThat(response.active()).isTrue();
    }

    @Test
    void create_throwsDuplicateResourceException_whenNameAlreadyExists() {
        DepartmentDtos.Request request = new DepartmentDtos.Request("PRODUCTION", true);
        when(repository.existsByNameIgnoreCase("PRODUCTION")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("PRODUCTION");
    }

    @Test
    void get_throwsResourceNotFoundException_whenIdDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_appliesChanges_toExistingDepartment() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentDtos.Response response = service.update(1L, new DepartmentDtos.Request("STORE", false));

        assertThat(response.name()).isEqualTo("STORE");
        assertThat(response.active()).isFalse();
        verify(repository).save(existing);
    }
}
