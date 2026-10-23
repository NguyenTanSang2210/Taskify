import { useState } from "react";
import Sidebar from "../components/Sidebar";
import TopBar from "../components/TopBar";
import { useMediaQuery } from "../hooks/useMediaQuery";
import { PORTAL_TITLES } from "../config/navigation";

/** Khung trang sau đăng nhập: menu trái, thanh trên, nội dung, chân trang. */
export default function AppLayout({ user, roleName, onLogout, children }) {
  const isDesktop = useMediaQuery("(min-width: 768px)");
  const [collapsed, setCollapsed] = useState(() => !window.matchMedia("(min-width: 1200px)").matches);
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-surface font-body">
      {/* MOBILE BACKDROP */}
      {mobileOpen && (
        <div className="fixed inset-0 bg-black/50 z-[1040] md:hidden backdrop-blur-sm" onClick={() => setMobileOpen(false)} />
      )}

      <Sidebar
        roleName={roleName}
        collapsed={collapsed}
        onToggleCollapsed={setCollapsed}
        mobileOpen={mobileOpen}
        onCloseMobile={() => setMobileOpen(false)}
        onLogout={onLogout}
      />

      <main className="flex-1 flex flex-col min-h-screen transition-all duration-300" style={{ marginLeft: isDesktop ? (collapsed ? 80 : 280) : 0 }}>
        <TopBar user={user} portalTitle={PORTAL_TITLES[roleName] || "Cổng học thuật"} onOpenMobile={() => setMobileOpen(true)} />

        {/* Page Canvas */}
        <div className="p-4 md:p-8 flex-1 bg-surface-container-lowest">
          <div className="max-w-7xl mx-auto w-full">{children}</div>
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
  );
}
