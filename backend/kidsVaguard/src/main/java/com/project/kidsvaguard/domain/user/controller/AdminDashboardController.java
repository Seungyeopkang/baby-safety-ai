package com.project.kidsvaguard.domain.user.controller;

import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.io.IOException;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminService adminService;

    public AdminDashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("userCount", adminService.countAllUsers());
        model.addAttribute("adminCount", adminService.countByRole(User.Role.ADMIN));
        model.addAttribute("userOnlyCount", adminService.countByRole(User.Role.USER));

        List<User> users = (keyword != null && !keyword.isEmpty())
                ? adminService.searchUsers(keyword)
                : adminService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);

        try {
            String logContent = Files.readString(Paths.get("logs/application.log"));
            model.addAttribute("logContent", logContent);
        } catch (IOException e) {
            model.addAttribute("logContent", "로그 파일을 읽을 수 없습니다.");
        }

        return "admin-dashboard";
    }
}
