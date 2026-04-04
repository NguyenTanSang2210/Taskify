package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.Privilege;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.repository.PrivilegeRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoleServiceTest {

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private PrivilegeRepository privilegeRepo;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateRole_Success() {
        String roleName = "NEW_ROLE";
        when(roleRepo.findByName(roleName)).thenReturn(Optional.empty());
        when(roleRepo.save(any(Role.class))).thenAnswer(i -> {
            Role r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        Role created = roleService.createRole(roleName, "Description");

        assertNotNull(created);
        assertEquals(roleName, created.getName());
        verify(auditLogService, times(1)).log(eq("CREATE_ROLE"), anyString(), anyString(), anyString());
    }

    @Test
    void testCreateRole_AlreadyExists() {
        String roleName = "EXISTING";
        when(roleRepo.findByName(roleName)).thenReturn(Optional.of(new Role(roleName)));

        assertThrows(ResponseStatusException.class, () -> {
            roleService.createRole(roleName, "Desc");
        });
    }

    @Test
    void testUpdateRole_SetPrivileges() {
        Role role = new Role("LECTURER");
        role.setId(1L);
        Privilege p = new Privilege("TOPIC_MANAGE");
        p.setId(10L);

        when(roleRepo.findById(1L)).thenReturn(Optional.of(role));
        when(privilegeRepo.findAllById(any())).thenReturn(Collections.singletonList(p));
        when(roleRepo.save(any(Role.class))).thenReturn(role);

        Role updated = roleService.updateRole(1L, "New Desc", Collections.singleton(10L));

        assertNotNull(updated);
        assertTrue(updated.getPrivileges().contains(p));
        verify(auditLogService, times(1)).log(eq("UPDATE_ROLE"), anyString(), anyString(), anyString());
    }
}
