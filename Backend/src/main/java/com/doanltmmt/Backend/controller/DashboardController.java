package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")

public class DashboardController {

    private final UserRepository userRepo;
    private final TopicRepository topicRepo;
    private final StudentRepository studentRepo;
    private final WorkspaceRepository workspaceRepo;
    private final SecurityScopeService scope;

    public DashboardController(UserRepository userRepo, TopicRepository topicRepo, StudentRepository studentRepo, WorkspaceRepository workspaceRepo, SecurityScopeService scope) {
        this.userRepo = userRepo;
        this.topicRepo = topicRepo;
        this.studentRepo = studentRepo;
        this.workspaceRepo = workspaceRepo;
        this.scope = scope;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepo.count());
        stats.put("totalTopics", topicRepo.count());
        stats.put("totalStudents", studentRepo.count());
        stats.put("openTopics", topicRepo.countByStatus("OPEN"));
        
        return stats;
    }

    @GetMapping("/lecturer")
    @PreAuthorize("hasRole('LECTURER')")
    public Map<String, Object> getLecturerStats() {
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.doanltmmt.Backend.entity.User user = userRepo.findByUsername(currentUsername).orElse(null);
        
        Map<String, Object> stats = new HashMap<>();
        if (user != null) {
            stats.put("myTopics", topicRepo.countByLecturer_User_Id(user.getId()));
        }
        return stats;
    }

    @GetMapping("/department-admin")
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_ADMIN')")
    public Map<String, Object> getDepartmentAdminStats() {
        Long deptId = scope.requireCurrentUser().getDepartment() != null ? scope.requireCurrentUser().getDepartment().getId() : null;
        if (deptId == null) {
            return Map.of(
                    "departmentId", null,
                    "totalWorkspaces", 0,
                    "totalUsers", 0,
                    "totalStudents", 0,
                    "totalTopics", 0,
                    "openTopics", 0
            );
        }
        scope.requireDepartmentAccess(deptId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("departmentId", deptId);
        stats.put("totalWorkspaces", workspaceRepo.countByDepartment_Id(deptId));
        stats.put("totalUsers", userRepo.countByDepartment_Id(deptId));
        stats.put("totalStudents", studentRepo.countByUser_Department_Id(deptId));
        stats.put("totalTopics", topicRepo.countByWorkspace_Department_Id(deptId));
        stats.put("openTopics", topicRepo.countByWorkspace_Department_IdAndStatus(deptId, "OPEN"));
        return stats;
    }
}
