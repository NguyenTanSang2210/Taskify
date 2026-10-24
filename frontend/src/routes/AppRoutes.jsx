import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import RequireRole from "../components/RequireRole";

// P3-11: mỗi trang là một chunk riêng, chỉ tải khi mở (bundle ban đầu nhỏ)
const StudentTopicsPage = lazy(() => import("../pages/StudentTopicPage"));
const StudentProgressPage = lazy(() => import("../pages/StudentProgressPage"));
const StudentMyRegistrationsPage = lazy(() => import("../pages/StudentMyRegistrationsPage"));
const StudentProgressDetailPage = lazy(() => import("../pages/StudentProgressDetailPage"));
const LecturerRegistrationsPage = lazy(() => import("../pages/LecturerRegistrationPage"));
const LecturerTopicsPage = lazy(() => import("../pages/LecturerTopicsPage"));
const LecturerProgressPage = lazy(() => import("../pages/LecturerProgressPage"));
const NotificationsPage = lazy(() => import("../pages/NotificationsPage"));
const AdminPage = lazy(() => import("../pages/AdminPage"));
const RoleManagementPage = lazy(() => import("../pages/RoleManagementPage"));
const AuditLogPage = lazy(() => import("../pages/AuditLogPage"));
const DepartmentAdminPage = lazy(() => import("../pages/DepartmentAdminPage"));
const ProfilePage = lazy(() => import("../pages/ProfilePage"));
const DashboardPage = lazy(() => import("../pages/DashboardPage"));
const ProjectSpacePage = lazy(() => import("../pages/ProjectSpacePage"));
const ProjectChatPage = lazy(() => import("../pages/ProjectChatPage"));
const CalendarPage = lazy(() => import("../pages/CalendarPage"));
const DiscussionPage = lazy(() => import("../pages/DiscussionPage"));
const TopicDetailPage = lazy(() => import("../pages/TopicDetailPage"));
const LoginPage = lazy(() => import("../pages/LoginPage"));

export function PageFallback() {
  return (
    <div className="py-24 flex justify-center" role="status" aria-label="Đang tải trang">
      <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent"></div>
    </div>
  );
}

const only = (role, page) => <RequireRole role={role}>{page}</RequireRole>;

/** Chưa đăng nhập: chỉ có trang đăng nhập. */
export function PublicRoutes() {
  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Suspense>
  );
}

export default function AppRoutes({ roleName }) {
  const isStaff = roleName === "ADMIN" || roleName === "LECTURER" || roleName === "DEPARTMENT_ADMIN";
  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        <Route path="/login" element={<Navigate to="/" replace />} />
        <Route path="/" element={isStaff ? <DashboardPage /> : <StudentTopicsPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/progress" element={only("STUDENT", <StudentProgressPage />)} />
        <Route path="/my-registrations" element={only("STUDENT", <StudentMyRegistrationsPage />)} />
        <Route path="/progress-detail" element={only("STUDENT", <StudentProgressDetailPage />)} />
        <Route path="/lecturer" element={only("LECTURER", <LecturerRegistrationsPage />)} />
        <Route path="/lecturer-topics" element={only("LECTURER", <LecturerTopicsPage />)} />
        <Route path="/lecturer-progress" element={only("LECTURER", <LecturerProgressPage />)} />
        <Route path="/notifications" element={only(roleName || "STUDENT", <NotificationsPage />)} />
        <Route
          path="/admin"
          element={only(["ADMIN", "DEPARTMENT_ADMIN"],
            roleName === "DEPARTMENT_ADMIN" ? <Navigate to="/department-admin" replace /> : <AdminPage />)}
        />
        <Route path="/admin/roles" element={only("ADMIN", <RoleManagementPage />)} />
        <Route path="/admin/audit" element={only("ADMIN", <AuditLogPage />)} />
        <Route path="/department-admin" element={only("DEPARTMENT_ADMIN", <DepartmentAdminPage />)} />
        <Route path="/project-space" element={only(["STUDENT", "LECTURER"], <ProjectSpacePage />)} />
        <Route path="/project-chat" element={only(["STUDENT", "LECTURER"], <ProjectChatPage />)} />
        <Route path="/calendar" element={only(["STUDENT", "LECTURER"], <CalendarPage />)} />
        <Route path="/discussion" element={only(["STUDENT", "LECTURER"], <DiscussionPage />)} />
        <Route path="/topics/:id" element={only(roleName || "STUDENT", <TopicDetailPage />)} />
        <Route path="/profile" element={only(roleName || "STUDENT", <ProfilePage />)} />
      </Routes>
    </Suspense>
  );
}
