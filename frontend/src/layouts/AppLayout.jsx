import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import TopBar from "../components/TopBar";
import { useMediaQuery } from "../hooks/useMediaQuery";
import { PORTAL_TITLES } from "../config/navigation";

/** Khung trang sau đăng nhập: menu trái, thanh trên, nội dung, chân trang. */
export default function AppLayout({ user, roleName, onLogout, children }) {
  const isDesktop = useMediaQuery("(min-width: 768px)");
  const [collapsed, setCollapsed] = useState(() => !window.matchMedia("(min-width: 1200px)").matches);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { pathname } = useLocation();
  const mainRef = useRef(null);
  const firstRender = useRef(true);

  // P4-11 (a11y): đổi trang trong SPA thì đưa focus vào nội dung mới (trình đọc màn hình đọc từ đầu trang mới)
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    mainRef.current?.focus({ preventScroll: true });
    window.scrollTo(0, 0);
  }, [pathname]);

  return (
    <div className="flex min-h-screen bg-surface font-body">
      <a href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[2000] focus:px-4 focus:py-2 focus:rounded-xl focus:bg-primary focus:text-on-primary focus:font-bold">
        Bỏ qua tới nội dung chính
      </a>
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

      <div className="flex-1 flex flex-col min-h-screen min-w-0 transition-all duration-300" style={{ marginLeft: isDesktop ? (collapsed ? 80 : 280) : 0 }}>
        <TopBar user={user} portalTitle={PORTAL_TITLES[roleName] || "Cổng học thuật"} onOpenMobile={() => setMobileOpen(true)} />

        <main id="main-content" ref={mainRef} tabIndex={-1} className="p-4 md:p-8 flex-1 bg-surface-container-lowest outline-none">
          <div className="max-w-7xl mx-auto w-full">{children}</div>
        </main>

        {/* Footer Shell */}
        <footer className="w-full bg-surface-container-low border-t border-outline-variant/10 py-4 px-8 mt-auto hidden md:flex justify-between items-center text-outline">
          <p className="text-[10px] font-bold uppercase tracking-widest">© {new Date().getFullYear()} Taskify — Hệ thống quản lý đề tài và tiến độ đồ án</p>
        </footer>
      </div>
    </div>
  );
}
