package com.doanltmmt.Backend.security.ratelimit;

/**
 * Kho bộ đếm cửa sổ cố định của {@link com.doanltmmt.Backend.security.RateLimiter}.
 * Mặc định trong RAM; chạy nhiều instance thì dùng {@code app.rate-limit.store=jdbc} để mọi instance đếm chung (P4-10).
 */
public interface RateLimitStore {

    /** Số lần đã ghi nhận trong cửa sổ hiện tại (tính cả lần này) và thời điểm cửa sổ kết thúc. */
    record Hit(int count, long windowEndMillis) {
    }

    /** Ghi nhận một lần cho {@code key}; cửa sổ cũ đã hết hạn thì mở cửa sổ mới dài {@code windowMillis}. */
    Hit hit(String key, long nowMillis, long windowMillis);

    /** Xóa các cửa sổ đã hết hạn. */
    void evictExpired(long nowMillis);

    /** Xóa toàn bộ bộ đếm (dùng cho test). */
    void clear();

    /** Số cửa sổ đang lưu (giám sát / test). */
    int size();
}
