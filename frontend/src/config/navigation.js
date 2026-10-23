// Menu bên trái theo vai trò (dữ liệu thay cho ~15 khối NavLink lặp lại trong App.jsx — P3-11)
export const DASHBOARD_ITEM = { to: "/dashboard", icon: "dashboard", label: "Tổng quan" };

export const ROLE_SECTIONS = {
  STUDENT: {
    title: "Sinh viên",
    items: [
      { to: "/", icon: "menu_book", label: "Danh sách đề tài" },
      { to: "/my-registrations", icon: "verified", label: "Đăng ký của tôi" },
      { to: "/project-space", icon: "tactic", label: "Không gian làm việc" },
      { to: "/project-chat", icon: "chat_bubble", label: "Trao đổi học thuật" },
    ],
  },
  LECTURER: {
    title: "Giảng viên",
    items: [
      { to: "/lecturer-topics", icon: "folder_open", label: "Quản lý đề tài" },
      { to: "/lecturer", icon: "rule", label: "Duyệt đăng ký" },
      { to: "/project-space", icon: "tactic", label: "Không gian làm việc" },
      { to: "/project-chat", icon: "chat_bubble", label: "Kênh trao đổi" },
    ],
  },
  ADMIN: {
    title: "Quản trị",
    items: [
      { to: "/admin", icon: "shield_person", label: "Quản trị hệ thống", end: true },
      { to: "/admin/roles", icon: "admin_panel_settings", label: "Vai trò & quyền hạn" },
      { to: "/admin/audit", icon: "history", label: "Nhật ký hệ thống" },
    ],
  },
  DEPARTMENT_ADMIN: {
    title: "Quản trị khoa",
    items: [{ to: "/department-admin", icon: "account_tree", label: "Quản trị khoa" }],
  },
};

export const PROFILE_ITEM = { to: "/profile", icon: "account_circle", label: "Hồ sơ của tôi" };

export const PORTAL_TITLES = {
  STUDENT: "Cổng học tập sinh viên",
  LECTURER: "Cổng học thuật giảng viên",
  DEPARTMENT_ADMIN: "Cổng điều phối khoa",
  ADMIN: "Cổng quản trị hệ thống",
};
