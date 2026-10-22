import React, { createContext, useState, useContext, useEffect } from 'react';

const AuthContext = createContext(null);

/** Thời điểm hết hạn (ms) đọc từ claim `exp` của JWT; null nếu không đọc được. */
const tokenExpiresAt = (jwt) => {
    try {
        const payload = jwt.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        const exp = JSON.parse(atob(payload)).exp;
        return typeof exp === "number" ? exp * 1000 : null;
    } catch {
        return null;
    }
};

const expireSession = () => {
    try {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
    } catch { void 0; }
    window.location.assign("/login?reason=expired");
};

export const AuthProvider = ({ children }) => {
    const [token, setToken] = useState(() => {
        try {
            return localStorage.getItem("token");
        } catch {
            return null;
        }
    });
    const [user, setUser] = useState(() => {
        try {
            const rawToken = localStorage.getItem("token");
            if (!rawToken) return null;
            const raw = localStorage.getItem("user");
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    });

    const login = (userData, token) => {
        localStorage.setItem("token", token);
        localStorage.setItem("user", JSON.stringify(userData));
        setToken(token);
        setUser(userData);
    };

    const logout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        localStorage.removeItem("temp_user");
        localStorage.removeItem("temp_token");
        setToken(null);
        setUser(null);
    };

    const updateUser = (newUserData) => {
        localStorage.setItem("user", JSON.stringify(newUserData));
        setUser(newUserData);
    };

    // Thay token của phiên hiện tại (ví dụ sau khi đổi mật khẩu, token cũ bị vô hiệu và server cấp token mới)
    const updateToken = (newToken) => {
        if (!newToken) return;
        localStorage.setItem("token", newToken);
        setToken(newToken);
    };

    // Tự đăng xuất đúng lúc token hết hạn (không đợi tới request kế tiếp bị 401)
    useEffect(() => {
        if (!token) return undefined;
        const expiresAt = tokenExpiresAt(token);
        if (!expiresAt) return undefined;
        const remaining = expiresAt - Date.now();
        if (remaining <= 0) {
            expireSession();
            return undefined;
        }
        // setTimeout chỉ nhận tối đa ~24,8 ngày
        const timer = setTimeout(expireSession, Math.min(remaining, 2147483647));
        return () => clearTimeout(timer);
    }, [token]);

    // Listen for storage events (other tabs)
    useEffect(() => {
        const handleStorageChange = (e) => {
            if (e.key === 'user') {
                try {
                    if (!localStorage.getItem("token")) {
                        setUser(null);
                        return;
                    }
                    setUser(e.newValue ? JSON.parse(e.newValue) : null);
                } catch {
                    setUser(null);
                }
            } else if (e.key === 'token') {
                setToken(e.newValue);
                if (!e.newValue) setUser(null);
            }
        };
        window.addEventListener('storage', handleStorageChange);
        return () => window.removeEventListener('storage', handleStorageChange);
    }, []);

    return (
        <AuthContext.Provider value={{ user, token, login, logout, updateUser, updateToken }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
