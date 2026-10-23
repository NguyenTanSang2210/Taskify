// Thanh phân trang dùng chung cho các API trả { items, page, size, totalElements, totalPages } (P3-06).
export default function Pagination({ page = 0, totalPages = 0, totalElements, onChange, disabled = false }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements != null ? (
      <p className="text-[10px] font-black uppercase tracking-widest text-outline text-right px-2">{totalElements} mục</p>
    ) : null;
  }
  const btn =
    "w-9 h-9 rounded-xl border border-outline-variant/20 bg-white flex items-center justify-center hover:bg-surface-container disabled:opacity-30 disabled:cursor-not-allowed transition-all";
  return (
    <nav className="flex items-center justify-end gap-3 px-2" aria-label="Phân trang">
      <span className="text-[10px] font-black uppercase tracking-widest text-outline">
        Trang {page + 1}/{totalPages}
        {totalElements != null && ` · ${totalElements} mục`}
      </span>
      <button type="button" className={btn} disabled={disabled || page <= 0} onClick={() => onChange(page - 1)} aria-label="Trang trước">
        <span className="material-symbols-outlined text-lg">chevron_left</span>
      </button>
      <button type="button" className={btn} disabled={disabled || page >= totalPages - 1} onClick={() => onChange(page + 1)} aria-label="Trang sau">
        <span className="material-symbols-outlined text-lg">chevron_right</span>
      </button>
    </nav>
  );
}
