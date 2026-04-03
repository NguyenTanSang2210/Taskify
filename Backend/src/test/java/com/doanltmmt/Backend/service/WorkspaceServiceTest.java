package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.dto.WorkspaceDTO;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.entity.Workspace;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.WorkspaceClassRepository;
import com.doanltmmt.Backend.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkspaceServiceTest {

    @Mock private WorkspaceRepository repo;
    @Mock private DepartmentRepository deptRepo;
    @Mock private TopicRepository topicRepo;
    @Mock private WorkspaceClassRepository workspaceClassRepo;
    @Mock private SecurityScopeService scope;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private WorkspaceService workspaceService;

    private User admin;
    private Department dept;

    @BeforeEach
    void setUp() {
        dept = new Department();
        dept.setId(1L);
        dept.setName("IT");

        Role role = new Role();
        role.setName("ADMIN");

        admin = new User();
        admin.setId(1L);
        admin.setRole(role);
        admin.setDepartment(dept);
    }

    @Test
    void list_AsAdmin_ShouldReturnAll() {
        when(scope.hasRole("ADMIN")).thenReturn(true);
        when(repo.findAll()).thenReturn(List.of(new Workspace()));

        List<WorkspaceDTO> result = workspaceService.list(null);

        assertEquals(1, result.size());
        verify(repo).findAll();
    }

    @Test
    void list_ByDepartment_ShouldVerifyAccess() {
        Long deptId = 2L;
        workspaceService.list(deptId);
        verify(scope).requireDepartmentAccess(deptId);
        verify(repo).findByDepartment_IdOrderByIdDesc(deptId);
    }

    @Test
    void transition_InvalidFlow_ShouldThrowException() {
        Workspace w = new Workspace();
        w.setId(10L);
        w.setStatus("DRAFT");
        w.setDepartment(dept);

        when(repo.findById(10L)).thenReturn(Optional.of(w));

        // DRAFT to CLOSED is not in valid flow (based on service code)
        assertThrows(ResponseStatusException.class, () -> {
            workspaceService.transition(10L, "CLOSED");
        });
    }

    @Test
    void transition_ValidFlow_ShouldUpdateStatus() {
        Workspace w = new Workspace();
        w.setId(10L);
        w.setStatus("DRAFT");
        w.setDepartment(dept);

        when(repo.findById(10L)).thenReturn(Optional.of(w));
        when(repo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        WorkspaceDTO result = workspaceService.transition(10L, "OPEN_TOPIC");

        assertEquals("OPEN_TOPIC", result.getStatus());
        verify(auditLogService).log(eq("TRANSITION_WORKSPACE"), any(), eq("10"), any());
    }
}
