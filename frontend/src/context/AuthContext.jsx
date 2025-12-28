import React, { createContext, useState, useContext, useEffect } from 'react';

const AuthContext = createContext(null);

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
        <AuthContext.Provider value={{ user, token, login, logout, updateUser }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
