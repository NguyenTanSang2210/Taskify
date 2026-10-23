package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.MessageRequest;
import com.doanltmmt.Backend.dto.response.*;
import com.doanltmmt.Backend.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Tin nhắn trong phạm vi một đề tài: cả NGƯỜI GỬI lẫn NGƯỜI NHẬN đều phải là thành viên của đề tài
 * (trước đây gửi được cho bất kỳ người dùng nào trong hệ thống).
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @GetMapping("/inbox")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> inbox() {
        return service.inbox();
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public MessageDTO send(@Valid @RequestBody MessageRequest body) {
        return service.send(body);
    }
}
