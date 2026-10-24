package com.doanltmmt.Backend.dto.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tên tiếng Việt của một trường trong request DTO — dùng cho thông báo lỗi khi giá trị sai kiểu
 * (vd. {@code "deadline": "31/12"} → "Hạn chót không đúng định dạng ngày giờ"). Không gắn thì dùng tên trường JSON.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldLabel {
    String value();
}
