package com.ameya.inventory.service;

import com.ameya.inventory.dto.user.UserDtos;
import com.ameya.inventory.entity.Employee;
import com.ameya.inventory.entity.Role;
import com.ameya.inventory.entity.User;
import com.ameya.inventory.exception.DuplicateResourceException;
import com.ameya.inventory.exception.ResourceNotFoundException;
import com.ameya.inventory.repository.EmployeeRepository;
import com.ameya.inventory.repository.RoleRepository;
import com.ameya.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserDtos.Response> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserDtos.Response get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public UserDtos.Response create(UserDtos.CreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken.");
        }
        Role role = findRole(request.roleName());

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);
        applyEmployee(user, request.employeeId());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserDtos.Response update(Long id, UserDtos.UpdateRequest request) {
        User user = findOrThrow(id);
        user.setRole(findRole(request.roleName()));
        user.setActive(request.active());
        applyEmployee(user, request.employeeId());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = findOrThrow(id);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = findOrThrow(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void applyEmployee(User user, Long employeeId) {
        if (employeeId == null) {
            user.setEmployee(null);
            return;
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", employeeId));
        user.setEmployee(employee);
    }

    private Role findRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + roleName));
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private UserDtos.Response toResponse(User user) {
        return new UserDtos.Response(
                user.getId(),
                user.getUsername(),
                user.getEmployee() != null ? user.getEmployee().getId() : null,
                user.getEmployee() != null ? user.getEmployee().getName() : null,
                user.getRole().getName(),
                user.isActive(),
                user.getLastLoginAt()
        );
    }
}
