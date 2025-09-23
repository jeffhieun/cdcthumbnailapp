package com.cathay.cdc.thumbnail.poc.service;

import com.cathay.cdc.thumbnail.poc.entity.User;
import com.cathay.cdc.thumbnail.poc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

//    public UserService(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }

    public List<User> findAll() {
        log.info("Fetching all users from database...");
        List<User> users = userRepository.findAll();
        log.info("Found {} users", users.size());
        return users;
    }
}
