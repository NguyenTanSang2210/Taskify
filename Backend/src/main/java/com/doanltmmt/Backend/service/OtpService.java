package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.OtpToken;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.OtpTokenRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {
    private final OtpTokenRepository repo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpTokenRepository repo, UserRepository userRepo, EmailService emailService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    public void sendOtp(String username) {
        User user = userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpToken token = new OtpToken();
        token.setUser(user);
        token.setCode(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);
        repo.save(token);
        emailService.sendEmail(user.getEmail(), "OTP Verification", "Your OTP code is: " + code + " (valid for 5 minutes)");
    }

    public boolean verifyOtp(String username, String code) {
        User user = userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return repo.findTopByUserAndCodeAndUsedFalseAndExpiresAtAfter(user, code, LocalDateTime.now())
                .map(t -> {
                    t.setUsed(true);
                    repo.save(t);
                    return true;
                }).orElse(false);
    }
}
