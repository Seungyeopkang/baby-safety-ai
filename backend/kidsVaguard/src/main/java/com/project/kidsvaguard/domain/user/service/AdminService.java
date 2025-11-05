package com.project.kidsvaguard.domain.user.service;

import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public long countAllUsers() {
        return userRepository.count();
    }

    public long countByRole(User.Role role) {
        return userRepository.countByRole(role);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.findByUserIdContainingIgnoreCase(keyword);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

}
