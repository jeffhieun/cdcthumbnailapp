package com.cathay.cdc.thumbnail.poc.controller;

import com.cathay.cdc.thumbnail.poc.entity.User;
import com.cathay.cdc.thumbnail.poc.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Received request to fetch all users");
        List<User> users = userService.findAll();
        if (users.isEmpty()) {
            log.warn("No users found in database");
            return ResponseEntity.noContent().build();
        }
        log.info("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }
}
