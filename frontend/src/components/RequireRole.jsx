import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function RequireRole({ role, children }) {
  const { user, token, ready } = useAuth();

  const userRole = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const expectedRoles = Array.isArray(role) ? role : [role];
  const normalizedExpected = expectedRoles
    .map((r) => (typeof r === "object" && r ? r.name : r))
    .filter(Boolean);

  // Đang khôi phục phiên (tải lại trang, P3-14): chưa quyết định chuyển hướng
  if (!ready) return null;

  if (!user || !token || !userRole || !normalizedExpected.includes(userRole)) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
