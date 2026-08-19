package com.ameya.inventory.service;

import com.ameya.inventory.dto.auth.LoginRequest;
import com.ameya.inventory.dto.auth.LoginResponse;
import com.ameya.inventory.repository.UserRepository;
import com.ameya.inventory.security.JwtService;
import com.ameya.inventory.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        userRepository.findById(principal.getId()).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });

        return new LoginResponse(token, principal.getUsername(), principal.getRole(), principal.getId());
    }
}
