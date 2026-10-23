package com.doanltmmt.Backend.dto.request;

import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;

/**
 * Dữ liệu tạo/sửa đề tài. Chỉ nhận đúng các trường người dùng được phép đặt — {@code id}, {@code status},
 * {@code lecturer} do hệ thống quyết định (trước đây body được bind thẳng vào entity {@link Topic}).
 * Đợt có thể gửi dạng {@code "workspace": {"id": 5}} (frontend hiện tại) hoặc {@code "workspaceId": 5}.
 */
public record TopicRequest(String title, String description, Integer capacity, WorkspaceRef workspace, Long workspaceId) {

    public record WorkspaceRef(Long id) {
    }

    public Topic toTopic() {
        Topic t = new Topic();
        t.setTitle(title);
        t.setDescription(description);
        t.setCapacity(capacity);
        Long wsId = workspaceId != null ? workspaceId : workspace != null ? workspace.id() : null;
        if (wsId != null) {
            Workspace w = new Workspace();
            w.setId(wsId);
            t.setWorkspace(w);
        }
        return t;
    }
}
