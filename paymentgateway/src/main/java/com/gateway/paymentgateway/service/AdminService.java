package com.gateway.paymentgateway.service;

import com.gateway.paymentgateway.dto.response.UserResponse;
import com.gateway.paymentgateway.entity.*;
import com.gateway.paymentgateway.repository.KycApprovalTokenRepository;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.gateway.paymentgateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepo;
    private final UserKycRepository kycRepo;
    private final KycApprovalTokenRepository tokenRepo;
    private final EmailService emailService;

    // =========================
    // USER MANAGEMENT
    // =========================

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getByEmail(String email) {
        return UserResponse.from(
                userRepo.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(
                userRepo.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found"))
        );
    }

    public void disableUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepo.save(user);
    }

    public void enableUser(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepo.save(user);
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    // =========================
    // KYC SECTION
    // =========================

    public UserKyc getUserKyc(Long userId) {
        return kycRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC not found"));
    }

    public void approveKyc(String token) {

        KycApprovalToken approval =
                tokenRepo.findByToken(token)
                        .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (approval.isUsed() ||
                approval.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired or already used");
        }

        User user = approval.getUser();
        UserKyc kyc = kycRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        if (kyc.getStatus() == KycStatus.APPROVED) {
            throw new RuntimeException("KYC already approved");
        }

        kyc.setStatus(KycStatus.APPROVED);
        user.setActive(true);
        approval.setUsed(true);

        kycRepo.save(kyc);
        userRepo.save(user);
        tokenRepo.save(approval);

        emailService.send(
                user.getEmail(),
                "KYC Approved",
                "Your KYC has been approved. You can now make payments securely."
        );
    }

    public void rejectKyc(String token, String reason) {

        KycApprovalToken approval =
                tokenRepo.findByToken(token)
                        .orElseThrow(() -> new RuntimeException("Invalid token"));

        User user = approval.getUser();
        UserKyc kyc = kycRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("KYC not found"));

        kyc.setStatus(KycStatus.REJECTED);
        approval.setUsed(true);

        kycRepo.save(kyc);
        tokenRepo.save(approval);

        emailService.send(
                user.getEmail(),
                "KYC Rejected",
                "Your KYC was rejected.\nReason: " + reason
        );
    }
}
