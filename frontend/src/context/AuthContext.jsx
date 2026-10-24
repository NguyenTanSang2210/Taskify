import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import { getAccessToken, onAccessTokenChange, refreshSession, revokeSession, setAccessToken, tokenExpiresAt } from '../auth/session';

const AuthContext = createContext(null);

// Chỉ lưu thông tin hiển thị (không nhạy cảm) để các tab đồng bộ đăng nhập/đăng xuất; token KHÔNG nằm ở đây (P3-14)
const USER_KEY = "user";
const LEGACY_KEYS = ["token", "temp_user", "temp_token"];

const toUser = (data) => ({ id: data.userId, username: data.username, fullName: data.fullName, role: data.role });

const readStoredUser = () => {
    try {
        const raw = localStorage.getItem(USER_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
};

const storeUser = (u) => {
    try {
        if (u) localStorage.setItem(USER_KEY, JSON.stringify(u));
        else localStorage.removeItem(USER_KEY);
    } catch { void 0; }
};

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(getAccessToken);
    const [user, setUser] = useState(null);
    // false cho tới khi biết có phiên hay không (tải lại trang → hỏi /auth/refresh)
    const [ready, setReady] = useState(false);

    useEffect(() => onAccessTokenChange(setToken), []);

    const clearLocal = useCallback(() => {
        setAccessToken(null);
        storeUser(null);
        setUser(null);
    }, []);

    // Khởi động: dọn token cũ trong localStorage (trước P3-14) và khôi phục phiên từ cookie refresh
    useEffect(() => {
        try { LEGACY_KEYS.forEach((k) => localStorage.removeItem(k)); } catch { void 0; }
        let cancelled = false;
        refreshSession()
            .then((data) => {
                if (cancelled) return;
                const u = { ...readStoredUser(), ...toUser(data) };
                storeUser(u);
                setUser(u);
            })
            .catch(() => {
                if (!cancelled) clearLocal();
            })
            .finally(() => {
                if (!cancelled) setReady(true);
            });
        return () => {
            cancelled = true;
        };
    }, [clearLocal]);

    const login = (userData, newToken) => {
        setAccessToken(newToken);
        storeUser(userData);
        setUser(userData);
    };

    const logout = useCallback(() => {
        revokeSession();
        clearLocal();
    }, [clearLocal]);

    const updateUser = (newUserData) => {
        storeUser(newUserData);
        setUser(newUserData);
    };

    // Thay access token của phiên hiện tại (vd. sau khi đổi mật khẩu: server cấp token + cookie refresh mới)
    const updateToken = (newToken) => {
        if (newToken) setAccessToken(newToken);
    };

    // Làm mới chủ động ~1 phút trước khi access token hết hạn (không đợi request kế tiếp bị 401)
    useEffect(() => {
        if (!token) return undefined;
        const expiresAt = tokenExpiresAt(token);
        if (!expiresAt) return undefined;
        const delay = Math.max(expiresAt - Date.now() - 60_000, 5_000);
        const timer = setTimeout(() => {
            refreshSession().catch(() => {
                clearLocal();
                window.location.assign("/login?reason=expired");
            });
        }, Math.min(delay, 2147483647));
        return () => clearTimeout(timer);
    }, [token, clearLocal]);

    // Đồng bộ giữa các tab: tab khác đăng xuất → tab này cũng đăng xuất; tab khác đăng nhập → nhận phiên qua cookie
    useEffect(() => {
        const handleStorageChange = (e) => {
            if (e.key !== USER_KEY) return;
            if (!e.newValue) {
                setAccessToken(null);
                setUser(null);
            } else if (!getAccessToken()) {
                refreshSession().then((data) => setUser({ ...JSON.parse(e.newValue), ...toUser(data) })).catch(() => {});
            }
        };
        window.addEventListener('storage', handleStorageChange);
        return () => window.removeEventListener('storage', handleStorageChange);
    }, []);

    return (
        <AuthContext.Provider value={{ user, token, ready, login, logout, updateUser, updateToken }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
