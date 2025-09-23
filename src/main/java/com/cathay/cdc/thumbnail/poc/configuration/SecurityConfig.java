package com.cathay.cdc.thumbnail.poc.configuration;

import com.cathay.cdc.thumbnail.poc.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.cathay.cdc.thumbnail.poc.entity.User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.stream.Collectors;

@Slf4j
@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for APIs
                .authorizeHttpRequests(auth -> auth
//                         Public endpoints (no authentication required)
                        .requestMatchers("/auth/*", "/register", "/health").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        log.info("✅ Security filter chain initialized");
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            log.info("🔑 Attempting to load user: {}", username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("❌ User not found: {}", username);
                        return new UsernameNotFoundException("User not found: " + username);
                    });

            log.info("✅ User found: {} (enabled={})", user.getUsername(), user.isEnabled());
            log.info("📌 Roles: {}", user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.joining(", ")));

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    user.isEnabled(),
                    true,
                    true,
                    true,
                    user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(role.getName())) // assumes ROLE_ADMIN / ROLE_USER in DB
                            .collect(Collectors.toSet())
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("✅ BCryptPasswordEncoder bean created");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        log.info("✅ AuthenticationManager bean created");
        return authenticationConfiguration.getAuthenticationManager();
    }
}
