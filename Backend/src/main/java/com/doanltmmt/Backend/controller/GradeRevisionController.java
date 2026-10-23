package com.doanltmmt.Backend.controller;

import com.doanltmmt.Backend.dto.request.GradeRevisionRequestBody;
import com.doanltmmt.Backend.dto.request.GradeRevisionReviewRequest;
import com.doanltmmt.Backend.dto.response.GradeHistoryDTO;
import com.doanltmmt.Backend.dto.response.GradeRevisionDTO;
import com.doanltmmt.Backend.dto.response.PageResponse;
import com.doanltmmt.Backend.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** P2-13 — yêu cầu chấm lại điểm và lịch sử điểm. */
@RestController
@RequestMapping("/api")
public class GradeRevisionController {

    private final GradeService gradeService;

    public GradeRevisionController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping("/registration/{regId}/grade-revisions")
    @PreAuthorize("hasRole('LECTURER')")
    public GradeRevisionDTO request(@PathVariable Long regId, @Valid @RequestBody GradeRevisionRequestBody body) {
        return gradeService.requestRevision(regId, body);
    }

    @GetMapping("/registration/{regId}/grade-history")
    @PreAuthorize("hasAnyRole('LECTURER','DEPARTMENT_ADMIN','ADMIN')")
    public List<GradeHistoryDTO> history(@PathVariable Long regId) {
        return gradeService.history(regId);
    }

    @GetMapping("/grade-revisions")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN','ADMIN')")
    public PageResponse<GradeRevisionDTO> list(@RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return gradeService.listRevisions(status, page, size);
    }

    @PostMapping("/grade-revisions/{id}/approve")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN','ADMIN')")
    public GradeRevisionDTO approve(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) GradeRevisionReviewRequest body) {
        return gradeService.approve(id, body == null ? GradeRevisionReviewRequest.empty() : body);
    }

    @PostMapping("/grade-revisions/{id}/reject")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN','ADMIN')")
    public GradeRevisionDTO reject(@PathVariable Long id,
                                   @Validated(GradeRevisionReviewRequest.Reject.class) @RequestBody GradeRevisionReviewRequest body) {
        return gradeService.reject(id, body);
    }
}
