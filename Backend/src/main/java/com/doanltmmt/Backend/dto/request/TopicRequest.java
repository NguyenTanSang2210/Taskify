package com.doanltmmt.Backend.dto.request;

import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.Workspace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu tạo/sửa đề tài. Chỉ nhận đúng các trường người dùng được phép đặt — {@code id}, {@code status},
 * {@code lecturer} do hệ thống quyết định (trước đây body được bind thẳng vào entity {@link Topic}).
 * Đợt có thể gửi dạng {@code "workspace": {"id": 5}} (frontend hiện tại) hoặc {@code "workspaceId": 5}.
 * Tên bắt buộc khi tạo và các quy tắc theo trạng thái đợt kiểm tra ở TopicService (khi sửa, trường không gửi giữ nguyên).
 */
public record TopicRequest(
        @Size(max = 255, message = "Tên đề tài phải từ 1 đến 255 ký tự") String title,
        @Size(max = 10_000, message = "Mô tả đề tài tối đa 10000 ký tự") String description,
        @FieldLabel("Số lượng sinh viên")
        @Min(value = 1, message = "Số lượng sinh viên phải từ 1 đến 10")
        @Max(value = 10, message = "Số lượng sinh viên phải từ 1 đến 10") Integer capacity,
        @Valid WorkspaceRef workspace,
        @Positive(message = "workspaceId không hợp lệ") Long workspaceId) {

    public record WorkspaceRef(@Positive(message = "workspaceId không hợp lệ") Long id) {
    }

    public TopicRequest {
        title = Texts.trim(title);
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
