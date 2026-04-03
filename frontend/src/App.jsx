// src/App.jsx
import "./App.css";
import { useMemo, useState } from "react";
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
import ProjectChatPage from "./pages/ProjectChatPage";
import StudentProgressDetailPage from "./pages/StudentProgressDetailPage";

export default function App() {
  const { user, token, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(() => window.innerWidth < 1200);
  const [mobileOpen, setMobileOpen] = useState(false);

  const roleName = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isDepartmentAdmin = roleName === "DEPARTMENT_ADMIN";
  
  // Logic for desktop margin
  const mainMarginLeft = window.innerWidth >= 768 ? (collapsed ? 76 : 284) : 0;
  const portalTitle = useMemo(() => {
    if (roleName === "STUDENT") return "Cổng học tập sinh viên";
    if (roleName === "LECTURER") return "Cổng học thuật giảng viên";
    if (isDepartmentAdmin) return "Cổng điều phối khoa";
    if (roleName === "ADMIN") return "Cổng quản trị hệ thống";
    return "Cổng học thuật";
  }, [roleName, isDepartmentAdmin]);

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
      <div className="flex min-h-screen bg-surface font-body">
        {/* MOBILE BACKDROP */}
        {mobileOpen && (
          <div 
            className="fixed inset-0 bg-black/50 z-[1040] md:hidden backdrop-blur-sm" 
            onClick={() => setMobileOpen(false)}
          />
        )}

        <aside
          className={`fixed left-0 top-0 h-full bg-white dark:bg-slate-950 flex flex-col py-6 px-4 gap-4 z-[1050] border-r border-outline-variant/10 transition-all duration-300 ease-in-out ${
            mobileOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
          }`}
          style={{ width: collapsed ? 80 : 280 }}
        >
          {/* Brand Header */}
          <div className="flex flex-col gap-1 px-2 mb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary rounded-xl flex items-center justify-center shadow-lg shadow-primary/20">
                <span className="material-symbols-outlined text-on-primary text-2xl" style={{ fontVariationSettings: "'FILL' 1" }}>
                  school
                </span>
              </div>
              {!collapsed && (
                <span className="text-xl font-black tracking-tight text-primary dark:text-blue-100 font-headline">
                  Taskify
                </span>
              )}
            </div>
            {!collapsed && (
              <p className="text-[10px] uppercase font-bold tracking-widest text-outline mt-3 ml-1">
                ACADEMIC WORKSPACE
              </p>
            )}
          </div>

          <nav className="flex-1 flex flex-col gap-1.5 overflow-y-auto no-scrollbar">
            <NavLink
              to="/dashboard"
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                  isActive 
                    ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                    : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                }`
              }
            >
              <span className="material-symbols-outlined">dashboard</span>
              {!collapsed && <span className="text-sm font-medium">Tổng quan</span>}
            </NavLink>

            {/* --- STUDENT --- */}
            {roleName === "STUDENT" && (
              <div className="flex flex-col gap-1.5 pt-4 border-t border-outline-variant/10">
                {!collapsed && <p className="text-[10px] uppercase font-bold tracking-widest text-outline mb-1 ml-4 italic">Sinh viên</p>}
                <NavLink
                  to="/"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">menu_book</span>
                  {!collapsed && <span className="text-sm font-medium">Danh sách đề tài</span>}
                </NavLink>
                <NavLink
                  to="/my-registrations"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">verified</span>
                  {!collapsed && <span className="text-sm font-medium">Đăng ký của tôi</span>}
                </NavLink>
                <NavLink
                  to="/project-space"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">tactic</span>
                  {!collapsed && <span className="text-sm font-medium">Không gian làm việc</span>}
                </NavLink>
                <NavLink
                  to="/project-chat"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">chat_bubble</span>
                  {!collapsed && <span className="text-sm font-medium">Trao đổi học thuật</span>}
                </NavLink>
              </div>
            )}

            {/* --- LECTURER --- */}
            {roleName === "LECTURER" && (
              <div className="flex flex-col gap-1.5 pt-4 border-t border-outline-variant/10">
                {!collapsed && <p className="text-[10px] uppercase font-bold tracking-widest text-outline mb-1 ml-4 italic">Giảng viên</p>}
                <NavLink
                  to="/lecturer-topics"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">folder_open</span>
                  {!collapsed && <span className="text-sm font-medium">Quản lý đề tài</span>}
                </NavLink>
                <NavLink
                  to="/lecturer"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">rule</span>
                  {!collapsed && <span className="text-sm font-medium">Duyệt đăng ký</span>}
                </NavLink>
                <NavLink
                  to="/project-space"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">tactic</span>
                  {!collapsed && <span className="text-sm font-medium">Không gian làm việc</span>}
                </NavLink>
                <NavLink
                  to="/project-chat"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">chat_bubble</span>
                  {!collapsed && <span className="text-sm font-medium">Kênh trao đổi</span>}
                </NavLink>
              </div>
            )}

            {/* --- ADMIN --- */}
            {roleName === "ADMIN" && (
              <div className="flex flex-col gap-1.5 pt-4 border-t border-outline-variant/10">
                {!collapsed && <p className="text-[10px] uppercase font-bold tracking-widest text-outline mb-1 ml-4 italic">Quản trị</p>}
                <NavLink
                  to="/admin"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">shield_person</span>
                  {!collapsed && <span className="text-sm font-medium">Quản trị hệ thống</span>}
                </NavLink>
              </div>
            )}

            {/* --- DEPARTMENT ADMIN --- */}
            {isDepartmentAdmin && (
              <div className="flex flex-col gap-1.5 pt-4 border-t border-outline-variant/10">
                {!collapsed && <p className="text-[10px] uppercase font-bold tracking-widest text-outline mb-1 ml-4 italic">Quản trị khoa</p>}
                <NavLink
                  to="/department-admin"
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                      isActive 
                        ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                        : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                    }`
                  }
                >
                  <span className="material-symbols-outlined">account_tree</span>
                  {!collapsed && <span className="text-sm font-medium">Quản trị khoa</span>}
                </NavLink>
              </div>
            )}
          </nav>

          <div className="mt-auto flex flex-col gap-1.5 border-t border-outline-variant/10 pt-4">
            {!collapsed && <p className="text-[10px] uppercase font-bold tracking-widest text-outline mb-1 ml-4 italic">Cá nhân</p>}
            <NavLink
              to="/profile"
              onClick={() => setMobileOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
                  isActive 
                    ? "bg-primary/10 text-primary font-bold border-r-4 border-primary" 
                    : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
                }`
              }
            >
              <span className="material-symbols-outlined">account_circle</span>
              {!collapsed && <span className="text-sm font-medium">Hồ sơ của tôi</span>}
            </NavLink>
            <button 
              onClick={handleLogout} 
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-error-dim dark:text-error hover:bg-error-container/10 transition-all duration-300 ease-out w-100 text-start"
            >
              <span className="material-symbols-outlined">logout</span>
              {!collapsed && <span className="text-sm font-medium">Đăng xuất</span>}
            </button>
          </div>

          {!collapsed && (
            <button
              className="mt-4 flex items-center justify-center p-2 rounded-xl border border-outline-variant/30 text-outline hover:text-primary hover:border-primary transition-all duration-300"
              onClick={() => setCollapsed(true)}
              aria-label="Collapse sidebar"
            >
              <span className="material-symbols-outlined text-lg">first_page</span>
            </button>
          )}
          {collapsed && (
            <button
              className="mt-4 flex items-center justify-center p-2 rounded-xl border border-outline-variant/30 text-outline hover:text-primary hover:border-primary transition-all duration-300"
              onClick={() => setCollapsed(false)}
              aria-label="Expand sidebar"
            >
              <span className="material-symbols-outlined text-lg">last_page</span>
            </button>
          )}
        </aside>

        <main 
          className="flex-1 flex flex-col min-h-screen transition-all duration-300" 
          style={{ marginLeft: window.innerWidth >= 768 ? (collapsed ? 80 : 280) : 0 }}
        >
          {/* TopAppBar Shell */}
          <header className="sticky top-0 z-40 bg-surface/80 dark:bg-slate-900/80 backdrop-blur-xl border-b border-outline-variant/10 flex justify-between items-center px-4 md:px-8 h-16 w-full">
            <div className="flex items-center gap-4 flex-1 max-w-xl">
              <button 
                className="p-2 rounded-full hover:bg-surface-container-high transition-colors md:hidden text-on-surface-variant" 
                onClick={() => setMobileOpen(true)}
              >
                <span className="material-symbols-outlined">menu</span>
              </button>
              <div className="relative flex-1 hidden sm:block">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-lg">search</span>
                <input 
                  className="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary/20 rounded-full pl-10 pr-4 py-2 text-sm transition-all focus:bg-white" 
                  placeholder="Tìm kiếm đồ án, tài liệu hoặc thành viên..." 
                  type="text"
                />
              </div>
            </div>

            <div className="flex items-center gap-2 md:gap-4">
              <div className="flex items-center gap-1">
                <NotificationsBell />
                <button className="p-2 rounded-xl hover:bg-surface-container-high transition-colors text-on-surface-variant">
                  <span className="material-symbols-outlined">help</span>
                </button>
              </div>

              <div className="h-8 w-[1px] bg-outline-variant/20 mx-1"></div>
              
              <div className="flex items-center gap-3 pl-1">
                <div className="text-right hidden sm:block">
                  <p className="text-[11px] font-black text-primary leading-none uppercase tracking-wider">{user?.fullName || "Người dùng"}</p>
                  <p className="text-[10px] text-outline font-bold mt-1 tracking-tight">{portalTitle}</p>
                </div>
                <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center border border-primary/20 shadow-sm overflow-hidden text-primary font-black text-sm">
                  {user?.fullName?.charAt(0) || "U"}
                </div>
              </div>
            </div>
          </header>

          {/* Page Canvas */}
          <div className="p-4 md:p-8 flex-1 bg-surface-container-lowest">
            <div className="max-w-7xl mx-auto w-full">
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
                <RequireRole role={["STUDENT", "LECTURER"]}>
                  <ProjectSpacePage />
                </RequireRole>
              }
            />
            <Route
              path="/project-chat"
              element={
                <RequireRole role={["STUDENT", "LECTURER"]}>
                  <ProjectChatPage />
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
            </div>
          </div>

          {/* Footer Shell */}
          <footer className="w-full bg-surface-container-low border-t border-outline-variant/10 py-4 px-8 mt-auto hidden md:flex justify-between items-center text-outline">
            <p className="text-[10px] font-bold uppercase tracking-widest opacity-60">© 2024 Taskify: Academic Management System</p>
            <div className="flex gap-6 text-[10px] uppercase font-bold tracking-widest">
              <a href="#" className="hover:text-primary transition-colors">Hỗ trợ</a>
              <a href="#" className="hover:text-primary transition-colors">Điều khoản</a>
              <a href="#" className="hover:text-primary transition-colors">Bảo mật</a>
            </div>
          </footer>
        </main>
      </div>
    </BrowserRouter>
  );
}
