package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.TopicRegistrationDTO;
import com.doanltmmt.Backend.service.TopicRegistrationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registration")

@SuppressWarnings("null")
public class TopicRegistrationController {

    private final TopicRegistrationService service;

    public TopicRegistrationController(TopicRegistrationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public TopicRegistrationDTO registerTopic(@RequestParam Long studentId,
                                           @RequestParam Long topicId) {
        return service.registerTopic(studentId, topicId);
    }

    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('LECTURER','DEPARTMENT_ADMIN','ADMIN')")
    public List<TopicRegistrationDTO> getRegistrations(@PathVariable Long topicId) {
        return service.getRegistrations(topicId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<TopicRegistrationDTO> myRegistrations(@RequestParam Long studentId) {
        return service.myRegistrations(studentId);
    }

    @PostMapping("/approve/{regId}")
    @PreAuthorize("hasRole('LECTURER')")
    public TopicRegistrationDTO approve(@PathVariable Long regId) {
        return service.approve(regId);
    }

    @PostMapping("/reject/{regId}")
    @PreAuthorize("hasRole('LECTURER')")
    public TopicRegistrationDTO reject(@PathVariable Long regId,
                                    @RequestParam(required = false) String reason) {
        return service.reject(regId, reason);
    }

    @PostMapping("/cancel/{regId}")
    @PreAuthorize("hasRole('STUDENT')")
    public void cancel(@PathVariable Long regId) {
        service.cancel(regId);
    }

    @PostMapping("/grade/{regId}")
    @PreAuthorize("hasRole('LECTURER')")
    public TopicRegistrationDTO grade(@PathVariable Long regId,
                                   @RequestBody Map<String, Object> body) {
        return service.grade(regId, body);
    }
}



