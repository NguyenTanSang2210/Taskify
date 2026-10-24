import NotificationsBell from "./NotificationsBell";

export default function TopBar({ user, portalTitle, onOpenMobile }) {
  return (
    <header className="sticky top-0 z-40 bg-surface/80 dark:bg-slate-900/80 backdrop-blur-xl border-b border-outline-variant/10 flex justify-between items-center px-4 md:px-8 h-16 w-full">
      <div className="flex items-center gap-4 flex-1 max-w-xl">
        <button aria-label="Mở menu"
          className="p-2 rounded-full hover:bg-surface-container-high transition-colors md:hidden text-on-surface-variant"
          onClick={onOpenMobile}
        >
          <span className="material-symbols-outlined">menu</span>
        </button>
      </div>

      <div className="flex items-center gap-2 md:gap-4">
        <div className="flex items-center gap-1">
          <NotificationsBell />
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
