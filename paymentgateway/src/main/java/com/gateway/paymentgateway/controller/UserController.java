package com.gateway.paymentgateway.controller;

import com.gateway.paymentgateway.dto.request.KycRequest;
import com.gateway.paymentgateway.entity.KycStatus;
import com.gateway.paymentgateway.entity.User;
import com.gateway.paymentgateway.entity.UserKyc;
import com.gateway.paymentgateway.repository.UserKycRepository;
import com.gateway.paymentgateway.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserKycRepository kycRepo;
    private final EmailService emailService;

    // ============================
    // GET USER PROFILE + KYC STATE
    // ============================
    @GetMapping("/profile")
    public Map<String, Object> getProfile(
            @AuthenticationPrincipal User user
    ) {

        UserKyc kyc = kycRepo.findByUserId(user.getId()).orElse(null);

        Map<String, Object> response = new HashMap<>();

        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("emailVerified", user.isEmailVerified());
        response.put("googleVerified", user.isGoogleVerified());
        response.put("createdAt", user.getCreatedAt());

        Map<String, Object> profile = new HashMap<>();
        profile.put("fatherName", kyc != null ? kyc.getFatherName() : "");
        profile.put("dob", kyc != null ? kyc.getDob() : "");
        profile.put("address", kyc != null ? kyc.getAddress() : "");
        profile.put("panNumber", kyc != null ? kyc.getPanNumber() : "");
        profile.put("bankAccount", kyc != null ? kyc.getBankAccount() : "");
        profile.put("ifsc", kyc != null ? kyc.getIfsc() : "");

        response.put("profile", profile);
        response.put("kycStatus", kyc != null ? kyc.getStatus() : KycStatus.NOT_SUBMITTED);

        response.put("editableFields", List.of(
                "fatherName",
                "dob",
                "address",
                "panNumber",
                "bankAccount",
                "ifsc"
        ));

        response.put("readOnlyFields", List.of(
                "email",
                "name"
        ));

        return response;
    }

    // ============================
    // SUBMIT / UPDATE KYC
    // ============================
    @PostMapping("/kyc")
    public String submitKyc(
            @AuthenticationPrincipal User user,
            @RequestBody KycRequest request
    ) {

        UserKyc kyc = kycRepo
                .findByUserId(user.getId())
                .orElse(new UserKyc());

        kyc.setUser(user);
        kyc.setFatherName(request.getFatherName());
        kyc.setDob(request.getDob());
        kyc.setAddress(request.getAddress());
        kyc.setPanNumber(request.getPanNumber());
        kyc.setBankAccount(request.getBankAccount());
        kyc.setIfsc(request.getIfsc());
        kyc.setStatus(KycStatus.PENDING);

        kycRepo.save(kyc);

        // 📩 Notify Admin
        emailService.send(
                "admin@yourapp.com",
                "New KYC Submitted",
                "User " + user.getEmail() + " has submitted KYC for verification."
        );

        // 📩 Notify User
        emailService.send(
                user.getEmail(),
                "KYC Submitted Successfully",
                "Your KYC has been submitted successfully and is under review."
        );

        return "KYC submitted successfully. Awaiting admin approval.";
    }
}
