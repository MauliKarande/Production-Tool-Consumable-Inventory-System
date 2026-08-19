package com.ameya.inventory.controller;

import com.ameya.inventory.dto.user.UserDtos;
import com.ameya.inventory.security.UserPrincipal;
import com.ameya.inventory.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public Page<UserDtos.Response> list(Pageable pageable) {
        return userService.list(pageable);
    }

    @GetMapping("/{id}")
    public UserDtos.Response get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDtos.Response create(@Valid @RequestBody UserDtos.CreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDtos.Response update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody UserDtos.ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
    }

    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public void changeOwnPassword(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request.currentPassword(), request.newPassword());
    }
}
