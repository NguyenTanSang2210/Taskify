// src/App.jsx
import "./App.css";
import { BrowserRouter } from "react-router-dom";
import { useAuth } from "./context/AuthContext";
import AppLayout from "./layouts/AppLayout";
import AppRoutes, { PublicRoutes } from "./routes/AppRoutes";

export default function App() {
  const { user, token, ready, logout } = useAuth();
  const roleName = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;

  // Đang khôi phục phiên từ cookie refresh (tải lại trang): chưa biết đăng nhập hay chưa
  if (!ready) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface" role="status" aria-label="Đang tải phiên đăng nhập">
        <div className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent"></div>
      </div>
    );
  }

  return (
    <BrowserRouter>
      {!user || !token ? (
        <PublicRoutes />
      ) : (
        <AppLayout user={user} roleName={roleName} onLogout={logout}>
          <AppRoutes roleName={roleName} />
        </AppLayout>
      )}
    </BrowserRouter>
  );
}
