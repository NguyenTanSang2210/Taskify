/** Bảng xem trước import: mỗi dòng Excel một hàng, dòng lỗi tô đỏ kèm danh sách lỗi. */
export default function ImportPreviewTable({ report, kind, onlyErrors }) {
    const rows = (report?.rows || []).filter((r) => !onlyErrors || r.errors.length > 0);
    const isStudent = kind !== "LECTURER";
    return (
        <div className="overflow-x-auto rounded-2xl border border-outline-variant/10">
            <table className="w-full text-left text-sm">
                <thead className="bg-surface-container-low text-[10px] font-black uppercase tracking-widest text-outline">
                    <tr>
                        <th className="px-4 py-3">Dòng</th>
                        <th className="px-4 py-3">{isStudent ? "MSSV" : "Mã giảng viên"}</th>
                        <th className="px-4 py-3">Họ tên</th>
                        <th className="px-4 py-3">Email</th>
                        {isStudent && <th className="px-4 py-3">Mã lớp</th>}
                        <th className="px-4 py-3">Kết quả kiểm tra</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/10">
                    {rows.map((r) => (
                        <tr key={r.row} className={r.errors.length ? "bg-error/5" : ""}>
                            <td className="px-4 py-3 font-mono text-xs text-outline">{r.row}</td>
                            <td className="px-4 py-3 font-bold">{r.username || "—"}</td>
                            <td className="px-4 py-3">{r.fullName || "—"}</td>
                            <td className="px-4 py-3">{r.email || "—"}</td>
                            {isStudent && <td className="px-4 py-3">{r.classCode || "—"}</td>}
                            <td className="px-4 py-3">
                                {r.errors.length === 0 ? (
                                    <span className="inline-flex items-center gap-1 text-xs font-bold text-green-700">
                                        <span className="material-symbols-outlined text-base" aria-hidden="true">check_circle</span>
                                        Hợp lệ
                                    </span>
                                ) : (
                                    <ul className="list-disc pl-4 text-xs font-bold text-error space-y-0.5">
                                        {r.errors.map((e) => <li key={e}>{e}</li>)}
                                    </ul>
                                )}
                            </td>
                        </tr>
                    ))}
                    {rows.length === 0 && (
                        <tr>
                            <td colSpan={isStudent ? 6 : 5} className="px-4 py-8 text-center text-xs font-bold text-outline">
                                Không có dòng nào để hiển thị.
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </div>
    );
}
