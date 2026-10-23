import NotificationsBell from "./NotificationsBell";

export default function TopBar({ user, portalTitle, onOpenMobile }) {
  return (
    <header className="sticky top-0 z-40 bg-surface/80 dark:bg-slate-900/80 backdrop-blur-xl border-b border-outline-variant/10 flex justify-between items-center px-4 md:px-8 h-16 w-full">
      <div className="flex items-center gap-4 flex-1 max-w-xl">
        <button
          className="p-2 rounded-full hover:bg-surface-container-high transition-colors md:hidden text-on-surface-variant"
          onClick={onOpenMobile}
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
  );
}
