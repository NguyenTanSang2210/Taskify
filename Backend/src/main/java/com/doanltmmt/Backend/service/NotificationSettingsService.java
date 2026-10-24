package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.request.NotificationSettingsRequest;
import com.doanltmmt.Backend.dto.response.NotificationSettingsDTO;
import com.doanltmmt.Backend.entity.NotificationSettings;
import com.doanltmmt.Backend.repository.EmailDigestItemRepository;
import com.doanltmmt.Backend.repository.NotificationSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** P4-06 — người dùng tự chọn nhóm email muốn nhận và gửi ngay hay tổng hợp hằng ngày. */
@Service
public class NotificationSettingsService {

    private final NotificationSettingsRepository repo;
    private final EmailDigestItemRepository digestRepo;
    private final SecurityScopeService scope;

    public NotificationSettingsService(NotificationSettingsRepository repo, EmailDigestItemRepository digestRepo, SecurityScopeService scope) {
        this.repo = repo;
        this.digestRepo = digestRepo;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public NotificationSettingsDTO mine() {
        Long userId = scope.requireCurrentUser().getId();
        return NotificationSettingsDTO.from(repo.findById(userId).orElseGet(() -> NotificationSettings.defaults(userId)),
                digestRepo.countByUser_Id(userId));
    }

    @Transactional
    public NotificationSettingsDTO update(NotificationSettingsRequest body) {
        Long userId = scope.requireCurrentUser().getId();
        NotificationSettings s = repo.findById(userId).orElseGet(() -> NotificationSettings.defaults(userId));
        s.setEmailDigest(body.emailDigest());
        s.setEmailRegistration(body.registration());
        s.setEmailProgress(body.progress());
        s.setEmailGrade(body.grade());
        s.setEmailReminder(body.reminder());
        s.setUpdatedAt(LocalDateTime.now());
        repo.save(s);
        return NotificationSettingsDTO.from(s, digestRepo.countByUser_Id(userId));
    }
}
