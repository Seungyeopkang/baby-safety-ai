package com.project.kidsvaguard.domain.user.controller;

import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.service.AdminService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminService adminService;

    // 대시보드 통계 정보 조회
    @GetMapping("/stats")
    public DashboardStats getDashboardStats() {
        return new DashboardStats(
                adminService.countAllUsers(),
                adminService.countByRole(User.Role.ADMIN),
                adminService.countByRole(User.Role.USER)
        );
    }

    // 유저 리스트 조회 (검색 포함)
    @GetMapping("/users")
    public List<User> getUsers(@RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return adminService.searchUsers(keyword);
        }
        return adminService.findAllUsers();
    }

    // 통계 응답용 DTO
    @Data
    @AllArgsConstructor
    static class DashboardStats {
        private long userCount;
        private long adminCount;
        private long userOnlyCount;
    }
}
