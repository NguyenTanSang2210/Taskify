import { useState } from "react";
import { fmtDeadline, submissionState } from "./milestoneUtils";

/** Lưới chấm theo mốc cho giảng viên: dòng = sinh viên, cột = mốc; bấm ô để nhập điểm (khi đợt đang thực hiện). */
export default function MilestoneGradeGrid({ board, onGrade, onEdit, onDelete, busy }) {
    const [editing, setEditing] = useState(null); // { milestoneId, studentId, score, feedback }
    const ms = board.milestones;
    const save = async (e) => {
        e.preventDefault();
        if (await onGrade(editing)) setEditing(null);
    };
    return (
        <div className="overflow-x-auto rounded-2xl border border-outline-variant/10">
            <table className="w-full text-left text-sm">
                <thead className="bg-surface-container-low text-[10px] font-black uppercase tracking-widest text-outline">
                    <tr>
                        <th className="px-4 py-3">Sinh viên</th>
                        {ms.map((m) => (
                            <th key={m.id} className="px-4 py-3 min-w-[11rem]">
                                <span className="block text-on-surface normal-case tracking-normal text-xs">{m.title}</span>
                                <span className="block font-bold normal-case tracking-normal">{fmtDeadline(m.deadline)} · {m.weight != null ? `${m.weight}%` : "không tính điểm"}</span>
                                <span className="flex gap-1 mt-1">
                                    <button type="button" onClick={() => onEdit(m)} className="text-primary hover:underline normal-case tracking-normal">Sửa</button>
                                    <span aria-hidden="true">·</span>
                                    <button type="button" onClick={() => onDelete(m)} className="text-error hover:underline normal-case tracking-normal">Xóa</button>
                                </span>
                            </th>
                        ))}
                        <th className="px-4 py-3">Điểm đề xuất</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/10">
                    {board.students.map((s) => (
                        <tr key={s.studentId}>
                            <td className="px-4 py-3"><span className="font-bold">{s.fullName}</span><span className="block text-xs text-outline">{s.studentCode}</span></td>
                            {s.cells.map((c, i) => {
                                const st = submissionState(c, ms[i]);
                                const open = editing && editing.milestoneId === c.milestoneId && editing.studentId === s.studentId;
                                return (
                                    <td key={c.milestoneId} className="px-4 py-3 align-top">
                                        <span className={`inline-block px-2 py-0.5 rounded-md text-[11px] font-bold ${st.tone}`}>{st.label}</span>
                                        {open ? (
                                            <form onSubmit={save} className="mt-2 space-y-2" aria-label={`Chấm ${ms[i].title} cho ${s.fullName}`}>
                                                <input type="number" min="0" max="10" step="0.25" required autoFocus value={editing.score}
                                                    onChange={(e) => setEditing({ ...editing, score: e.target.value })} aria-label="Điểm"
                                                    className="w-20 px-2 py-1.5 bg-surface-container-low rounded-lg text-sm font-bold" />
                                                <textarea rows={2} value={editing.feedback} onChange={(e) => setEditing({ ...editing, feedback: e.target.value })}
                                                    placeholder="Nhận xét" aria-label="Nhận xét" className="w-full px-2 py-1.5 bg-surface-container-low rounded-lg text-xs" />
                                                <div className="flex gap-2">
                                                    <button type="submit" disabled={busy} className="px-3 py-1.5 rounded-lg bg-primary text-white text-[10px] font-black uppercase">Lưu</button>
                                                    <button type="button" onClick={() => setEditing(null)} className="px-3 py-1.5 rounded-lg bg-surface-container text-[10px] font-black uppercase">Hủy</button>
                                                </div>
                                            </form>
                                        ) : (
                                            <button type="button" disabled={!board.canGrade}
                                                onClick={() => setEditing({ milestoneId: c.milestoneId, studentId: s.studentId, score: c.score ?? "", feedback: c.feedback ?? "" })}
                                                className="block mt-1.5 text-left disabled:cursor-default" title={board.canGrade ? "Bấm để chấm điểm" : "Chỉ chấm khi đợt đang thực hiện"}>
                                                <span className="text-lg font-black tabular-nums">{c.score ?? "—"}</span>
                                                {board.canGrade && <span className="ml-1 text-[10px] font-bold text-primary">{c.score == null ? "Chấm" : "Sửa"}</span>}
                                            </button>
                                        )}
                                    </td>
                                );
                            })}
                            <td className="px-4 py-3 align-top">
                                <span className="text-lg font-black tabular-nums">{s.weightedScore ?? "—"}</span>
                                {s.weightedScore != null && !s.complete && <span className="block text-[11px] font-bold text-outline">chưa chấm đủ các mốc</span>}
                            </td>
                        </tr>
                    ))}
                    {board.students.length === 0 && (
                        <tr><td colSpan={ms.length + 2} className="px-4 py-8 text-center text-xs font-bold text-outline">Đề tài chưa có sinh viên được duyệt.</td></tr>
                    )}
                </tbody>
            </table>
        </div>
    );
}
