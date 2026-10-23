package com.doanltmmt.Backend.entity.enums;

/**
 * Yêu cầu chấm lại: {@link #PENDING} → {@link #APPROVED} (mở khóa đến {@code unlockUntil}) → {@link #USED} khi GV sửa
 * điểm, hoặc {@link #EXPIRED} khi hết hạn; {@link #REJECTED} nếu bị từ chối.
 */
public enum GradeRevisionStatus {
    PENDING, APPROVED, REJECTED, USED, EXPIRED
}
