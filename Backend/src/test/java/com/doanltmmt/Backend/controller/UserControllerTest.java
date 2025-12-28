package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.DataSeeder;
import com.doanltmmt.Backend.config.SecurityConfig;
import com.doanltmmt.Backend.entity.Department;
import com.doanltmmt.Backend.entity.Role;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.AcademicClassRepository;
import com.doanltmmt.Backend.repository.DepartmentRepository;
import com.doanltmmt.Backend.repository.LecturerRepository;
import com.doanltmmt.Backend.repository.RoleRepository;
import com.doanltmmt.Backend.repository.StudentRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.security.JwtAuthenticationFilter;
import com.doanltmmt.Backend.service.AuditLogService;
import com.doanltmmt.Backend.service.CustomUserDetailsService;
import com.doanltmmt.Backend.service.SecurityScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;
    
    @MockBean
    private StudentRepository studentRepository;

    @MockBean
    private LecturerRepository lecturerRepository;

    @MockBean
    private DepartmentRepository departmentRepository;

    @MockBean
    private AcademicClassRepository academicClassRepository;
    
    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private SecurityScopeService scope;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private DataSeeder dataSeeder;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void getAllRoles_ShouldReturnRoles() throws Exception {
        Role r = new Role("STUDENT");
        given(roleRepository.findAll()).willReturn(List.of(r));

        mockMvc.perform(get("/api/users/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("STUDENT"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void updateStatus_ShouldUpdateActive() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setActive(true);
        
        given(userRepository.findById(1L)).willReturn(Optional.of(u));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/users/1/status")
                .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void createUser_ShouldCreateLecturerRole() throws Exception {
        Role lecturerRole = new Role("LECTURER");
        lecturerRole.setId(2L);
        Department dept = new Department();
        dept.setId(10L);

        given(userRepository.findByUsername("newlecturer")).willReturn(Optional.empty());
        given(userRepository.findByEmail("newlecturer@example.com")).willReturn(Optional.empty());
        given(roleRepository.findById(2L)).willReturn(Optional.of(lecturerRole));
        given(departmentRepository.findById(10L)).willReturn(Optional.of(dept));
        given(passwordEncoder.encode("123456")).willReturn("ENC");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });
        given(userRepository.getReferenceById(1L)).willAnswer(invocation -> {
            User ref = new User();
            ref.setId(1L);
            return ref;
        });
        given(lecturerRepository.existsById(1L)).willReturn(false);

        String payload = """
                {
                  "username": "newlecturer",
                  "password": "123456",
                  "fullName": "New Lecturer",
                  "email": "newlecturer@example.com",
                  "phone": "0123456789",
                  "roleId": 2,
                  "departmentId": 10,
                  "degree": "PhD",
                  "speciality": "AI"
                }
                """;

        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("newlecturer"))
                .andExpect(jsonPath("$.fullName").value("New Lecturer"));
    }
}
