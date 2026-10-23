import NavItem from "./NavItem";
import { DASHBOARD_ITEM, PROFILE_ITEM, ROLE_SECTIONS } from "../config/navigation";

const sectionTitle = "text-[10px] uppercase font-bold tracking-widest text-outline mb-1 ml-4 italic";
const toggleButton =
  "mt-4 flex items-center justify-center p-2 rounded-xl border border-outline-variant/30 text-outline hover:text-primary hover:border-primary transition-all duration-300";

export default function Sidebar({ roleName, collapsed, onToggleCollapsed, mobileOpen, onCloseMobile, onLogout }) {
  const section = ROLE_SECTIONS[roleName];
  return (
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
            <span className="text-xl font-black tracking-tight text-primary dark:text-blue-100 font-headline">Taskify</span>
          )}
        </div>
        {!collapsed && <p className="text-[10px] uppercase font-bold tracking-widest text-outline mt-3 ml-1">ACADEMIC WORKSPACE</p>}
      </div>

      <nav className="flex-1 flex flex-col gap-1.5 overflow-y-auto no-scrollbar">
        <NavItem {...DASHBOARD_ITEM} collapsed={collapsed} />
        {section && (
          <div className="flex flex-col gap-1.5 pt-4 border-t border-outline-variant/10">
            {!collapsed && <p className={sectionTitle}>{section.title}</p>}
            {section.items.map((item) => (
              <NavItem key={item.to} {...item} collapsed={collapsed} />
            ))}
          </div>
        )}
      </nav>

      <div className="mt-auto flex flex-col gap-1.5 border-t border-outline-variant/10 pt-4">
        {!collapsed && <p className={sectionTitle}>Cá nhân</p>}
        <NavItem {...PROFILE_ITEM} collapsed={collapsed} onClick={onCloseMobile} />
        <button
          onClick={onLogout}
          className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-error-dim dark:text-error hover:bg-error-container/10 transition-all duration-300 ease-out w-full text-left"
        >
          <span className="material-symbols-outlined">logout</span>
          {!collapsed && <span className="text-sm font-medium">Đăng xuất</span>}
        </button>
      </div>

      <button
        className={toggleButton}
        onClick={() => onToggleCollapsed(!collapsed)}
        aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
      >
        <span className="material-symbols-outlined text-lg">{collapsed ? "last_page" : "first_page"}</span>
      </button>
    </aside>
  );
}
