package com.cathay.cdc.thumbnail.poc.controller;

import com.cathay.cdc.thumbnail.poc.configuration.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        log.info("Login attempt for user: {}", username);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.get("password"))
            );
            List<String> roles = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toList());
            String token = jwtUtil.generateToken(username, roles);
            log.info("Login successful for user: {}, roles={}", username, roles);
            return ResponseEntity.ok(Map.of("token", token, "roles", roles));
        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", username, e);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", username, e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
