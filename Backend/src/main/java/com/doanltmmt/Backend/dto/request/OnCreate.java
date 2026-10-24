package com.doanltmmt.Backend.dto.request;


/**
 * Nhóm Bean Validation chỉ áp dụng khi tạo mới (trường bắt buộc khi tạo, nhưng được bỏ trống khi sửa từng phần).
 * Dùng: {@code @Validated({OnCreate.class, Default.class})} ở endpoint tạo.
 */
public interface OnCreate {
}
