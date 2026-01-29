package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.response.AdminUserResponse;
import com.gateway.paymentgateway.dto.response.UserResponse;
import com.gateway.paymentgateway.entity.UserKyc;
import com.gateway.paymentgateway.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ==============================
    // USER MANAGEMENT
    // ==============================

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/email/{email}")
    public UserResponse getByEmail(@PathVariable String email) {
        return adminService.getByEmail(email);
    }

    @GetMapping("/users/id/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return adminService.getById(id);
    }

    @PutMapping("/users/{id}/disable")
    public String disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return "User disabled successfully";
    }

    @PutMapping("/users/{id}/enable")
    public String enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return "User enabled successfully";
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return "User deleted successfully";
    }

    // ==============================
    // KYC SECTION
    // ==============================

    @GetMapping("/kyc/{userId}")
    public UserKyc getUserKyc(@PathVariable Long userId) {
        return adminService.getUserKyc(userId);
    }

    @PostMapping("/kyc/approve")
    public String approveKyc(@RequestParam String token) {
        adminService.approveKyc(token);
        return "KYC approved successfully";
    }

    @PostMapping("/kyc/reject")
    public String rejectKyc(@RequestParam String token,
                            @RequestParam String reason) {
        adminService.rejectKyc(token, reason);
        return "KYC rejected";
    }
}
