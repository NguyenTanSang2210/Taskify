package com.doanltmmt.Backend.entity.enums;

/** Trạng thái chấm điểm của một đăng ký (tính từ trạng thái đợt + yêu cầu chấm lại), trả cho giao diện. */
public enum GradeState {
    /** Đợt đang thực hiện — chấm/sửa tự do. */
    OPEN,
    /** Đợt đã đóng — điểm khóa. */
    LOCKED,
    /** Đợt đã đóng nhưng có yêu cầu chấm lại được duyệt, còn hạn. */
    UNLOCKED,
    /** Chưa đến giai đoạn chấm. */
    NOT_GRADING
}
