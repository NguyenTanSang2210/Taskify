// src/App.jsx
import "./App.css";
import { useState } from "react";
import { BrowserRouter, Routes, Route, Link, NavLink, Navigate } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import StudentTopicsPage from "./pages/StudentTopicPage";
import StudentProgressPage from "./pages/StudentProgressPage";
import StudentMyRegistrationsPage from "./pages/StudentMyRegistrationsPage";
import LecturerRegistrationsPage from "./pages/LecturerRegistrationPage";
import LecturerTopicsPage from "./pages/LecturerTopicsPage";
import LoginPage from "./pages/LoginPage";
import RequireRole from "./components/RequireRole";
import LecturerProgressPage from "./pages/LecturerProgressPage";
import NotificationsPage from "./pages/NotificationsPage";
import NotificationsBell from "./components/NotificationsBell";
import AdminPage from "./pages/AdminPage";
import DepartmentAdminPage from "./pages/DepartmentAdminPage";
import ProfilePage from "./pages/ProfilePage";
import DashboardPage from "./pages/DashboardPage";
import ProjectSpacePage from "./pages/ProjectSpacePage";
import StudentProgressDetailPage from "./pages/StudentProgressDetailPage";

export default function App() {
  const { user, token, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(() => {
    try {
      const w = window.innerWidth || 1024;
      return w < 768;
    } catch {
      return false;
    }
  });

  const roleName = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isDepartmentAdmin = roleName === "DEPARTMENT_ADMIN";

  const handleLogout = () => {
    logout();
    // window.location.assign("/login"); // No longer needed with context state change
  };

  if (!user || !token) {
    return (
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    );
  }

  return (
    <BrowserRouter>
      <div className="d-flex" style={{ minHeight: "100vh" }}>
        <aside
          className="bg-dark text-light d-flex flex-column p-3"
          style={{
            width: collapsed ? 64 : 240,
            position: "fixed",
            top: 0,
            bottom: 0,
            overflowY: "auto",
          }}
        >
          <div className="d-flex align-items-center mb-3">
            <button
              className="btn btn-outline-light btn-sm me-2"
              onClick={() => setCollapsed((c) => !c)}
              aria-label="Toggle sidebar"
              title="Thu gọn"
            >
              <i className="bi bi-list" />
            </button>
            {!collapsed && (
              <Link className="fw-bold text-decoration-none text-light" to="/">
                Đề tài LTMMT
              </Link>
            )}
          </div>
          {/* --- COMMON --- */}
          <ul className="nav nav-pills flex-column mb-3">
            <li className="nav-item">
              <NavLink
                to="/dashboard"
                className={({ isActive }) =>
                  `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                }
              >
                <i className="bi bi-speedometer2 me-2" />
                {!collapsed && <span>Tổng quan</span>}
              </NavLink>
            </li>
          </ul>

          {/* --- STUDENT --- */}
          {roleName === "STUDENT" && (
            <>
              {!collapsed && <div className="small text-muted fw-bold mb-2">Sinh viên</div>}
              <ul className="nav nav-pills flex-column mb-3">
                <li className="nav-item">
                  <NavLink
                    to="/"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-list-task me-2" />
                    {!collapsed && <span>Danh sách đề tài</span>}
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink
                    to="/my-registrations"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-journal-check me-2" />
                    {!collapsed && <span>Đăng ký của tôi</span>}
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink
                    to="/project-space"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-kanban me-2" />
                    {!collapsed && <span>Không gian làm việc</span>}
                  </NavLink>
                </li>
              </ul>
            </>
          )}

          {/* --- LECTURER --- */}
          {roleName === "LECTURER" && (
            <>
              {!collapsed && <div className="small text-muted fw-bold mb-2">Giảng viên</div>}
              <ul className="nav nav-pills flex-column mb-3">
                <li className="nav-item">
                  <NavLink
                    to="/lecturer-topics"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-folder2-open me-2" />
                    {!collapsed && <span>Quản lý đề tài</span>}
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink
                    to="/lecturer"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-clipboard-check me-2" />
                    {!collapsed && <span>Duyệt đăng ký</span>}
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink
                    to="/project-space"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-kanban me-2" />
                    {!collapsed && <span>Không gian làm việc</span>}
                  </NavLink>
                </li>
              </ul>
            </>
          )}

          {/* --- ADMIN --- */}
          {roleName === "ADMIN" && (
            <>
              {!collapsed && <div className="small text-muted fw-bold mb-2">Quản trị</div>}
              <ul className="nav nav-pills flex-column mb-3">
                <li className="nav-item">
                  <NavLink
                    to="/admin"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-shield-lock me-2" />
                    {!collapsed && <span>Quản trị hệ thống</span>}
                  </NavLink>
                </li>
              </ul>
            </>
          )}

          {/* --- DEPARTMENT ADMIN --- */}
          {isDepartmentAdmin && (
            <>
              {!collapsed && <div className="small text-muted fw-bold mb-2">Quản trị khoa</div>}
              <ul className="nav nav-pills flex-column mb-3">
                <li className="nav-item">
                  <NavLink
                    to="/department-admin"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-kanban me-2" />
                    {!collapsed && <span>Quản trị khoa</span>}
                  </NavLink>
                </li>
              </ul>
            </>
          )}

          <div className="mt-auto">
             {!collapsed && <div className="small text-muted fw-bold mb-2">Cá nhân</div>}
             <ul className="nav nav-pills flex-column">
               <li className="nav-item">
                  <NavLink
                    to="/profile"
                    className={({ isActive }) =>
                      `nav-link d-flex align-items-center ${isActive ? "active bg-secondary text-light" : "text-light"}`
                    }
                  >
                    <i className="bi bi-person-circle me-2" />
                    {!collapsed && <span>Hồ sơ của tôi</span>}
                  </NavLink>
               </li>
               <li className="nav-item">
                  <button onClick={handleLogout} className="nav-link text-light d-flex align-items-center w-100 text-start">
                    <i className="bi bi-box-arrow-right me-2" />
                    {!collapsed && <span>Đăng xuất</span>}
                  </button>
               </li>
             </ul>
          </div>
        </aside>
        <main className="flex-grow-1" style={{ marginLeft: collapsed ? 64 : 240 }}>
          <Routes>
            <Route path="/login" element={<Navigate to="/" replace />} />
            <Route path="/" element={roleName === "ADMIN" || roleName === "LECTURER" || isDepartmentAdmin ? <DashboardPage /> : <StudentTopicsPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route
              path="/progress"
              element={
                <RequireRole role="STUDENT">
                  <StudentProgressPage />
                </RequireRole>
              }
            />
            <Route
              path="/my-registrations"
              element={
                <RequireRole role="STUDENT">
                  <StudentMyRegistrationsPage />
                </RequireRole>
              }
            />
            <Route
              path="/lecturer"
              element={
                <RequireRole role="LECTURER">
                  <LecturerRegistrationsPage />
                </RequireRole>
              }
            />
            <Route
              path="/lecturer-topics"
              element={
                <RequireRole role="LECTURER">
                  <LecturerTopicsPage />
                </RequireRole>
              }
            />
            <Route
              path="/lecturer-progress"
              element={
                <RequireRole role="LECTURER">
                  <LecturerProgressPage />
                </RequireRole>
              }
            />
            <Route
              path="/notifications"
              element={
                <RequireRole role={roleName || "STUDENT"}>
                  <NotificationsPage />
                </RequireRole>
              }
            />
            <Route
              path="/admin"
              element={
                <RequireRole role={["ADMIN", "DEPARTMENT_ADMIN"]}>
                  {roleName === "DEPARTMENT_ADMIN" ? <Navigate to="/department-admin" replace /> : <AdminPage />}
                </RequireRole>
              }
            />
            <Route
              path="/department-admin"
              element={
                <RequireRole role="DEPARTMENT_ADMIN">
                  <DepartmentAdminPage />
                </RequireRole>
              }
            />
            <Route
              path="/project-space"
              element={
                <RequireRole role={roleName || "STUDENT"}>
                  <ProjectSpacePage />
                </RequireRole>
              }
            />
            <Route
              path="/progress-detail"
              element={
                <RequireRole role="STUDENT">
                  <StudentProgressDetailPage />
                </RequireRole>
              }
            />
            <Route
              path="/profile"
              element={
                <RequireRole role={roleName || "STUDENT"}>
                  <ProfilePage />
                </RequireRole>
              }
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
