package com.doanltmmt.Backend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Tham số phân trang từ client: trang âm → 0, kích thước kẹp trong 1..{@link #MAX_SIZE} (không bao giờ tải cả bảng). */
public final class Pages {

    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SIZE = "20";

    private Pages() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), sort);
    }

    public static Pageable of(int page, int size) {
        return of(page, size, Sort.unsorted());
    }
}
