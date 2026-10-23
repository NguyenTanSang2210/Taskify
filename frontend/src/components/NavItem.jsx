import { NavLink } from "react-router-dom";

const linkClass = ({ isActive }) =>
  `flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-300 ease-out ${
    isActive
      ? "bg-primary/10 text-primary font-bold border-r-4 border-primary"
      : "text-on-surface-variant hover:bg-surface-container-low hover:text-primary"
  }`;

/** Một mục menu bên trái: icon Material Symbols + nhãn (ẩn nhãn khi thu gọn). */
export default function NavItem({ to, icon, label, end, collapsed, onClick }) {
  return (
    <NavLink to={to} end={end} onClick={onClick} className={linkClass}>
      <span className="material-symbols-outlined">{icon}</span>
      {!collapsed && <span className="text-sm font-medium">{label}</span>}
    </NavLink>
  );
}
