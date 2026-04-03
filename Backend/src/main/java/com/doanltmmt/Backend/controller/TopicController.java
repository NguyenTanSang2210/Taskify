package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.TopicDTO;
import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.service.TopicService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@SuppressWarnings("null")
public class TopicController {

    private final TopicService service;

    public TopicController(TopicService service) {
        this.service = service;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public TopicDTO createTopic(@RequestParam Long lecturerId, @RequestBody Topic topic) {
        return service.createTopic(lecturerId, topic);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public TopicDTO updateTopic(@PathVariable Long id,
                             @RequestParam(required = false) Long lecturerId,
                             @RequestBody Topic payload) {
        return service.updateTopic(id, lecturerId, payload);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public void deleteTopic(@PathVariable Long id,
                            @RequestParam(required = false) Long lecturerId) {
        service.deleteTopic(id, lecturerId);
    }

    @PostMapping("/{id}/open")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public TopicDTO openTopic(@PathVariable Long id,
                           @RequestParam(required = false) Long lecturerId) {
        return service.openTopic(id, lecturerId);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public TopicDTO closeTopic(@PathVariable Long id,
                            @RequestParam(required = false) Long lecturerId) {
        return service.closeTopic(id, lecturerId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','LECTURER','ADMIN','DEPARTMENT_ADMIN')")
    public Map<String, Object> getAll(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false, name = "query") String queryText,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return service.getAll(studentId, queryText, status, lecturerId, page, size);
    }
}

