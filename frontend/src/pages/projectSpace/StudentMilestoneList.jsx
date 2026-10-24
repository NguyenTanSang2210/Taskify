import { Link } from "react-router-dom";
import { fmtDeadline, submissionState } from "./milestoneUtils";

/** Sinh viên: các mốc của đề tài, trạng thái nộp, điểm + nhận xét, nút nộp bài. */
export default function StudentMilestoneList({ board }) {
    const row = board.students[0];
    if (!row) return null;
    return (
        <ul className="divide-y divide-outline-variant/10">
            {board.milestones.map((m, i) => {
                const c = row.cells[i];
                const st = submissionState(c, m);
                return (
                    <li key={m.id} className="py-4 flex flex-col md:flex-row md:items-center gap-3 justify-between">
                        <div>
                            <p className="font-bold">{m.title} <span className="text-xs font-medium text-outline">· hạn {fmtDeadline(m.deadline)}{m.weight != null ? ` · ${m.weight}% điểm` : ""}</span></p>
                            {m.description && <p className="text-xs text-on-surface-variant mt-1 whitespace-pre-wrap">{m.description}</p>}
                            {c.feedback && <p className="text-xs mt-1"><span className="font-black text-outline">Nhận xét:</span> {c.feedback}</p>}
                        </div>
                        <div className="flex items-center gap-3 shrink-0">
                            <span className={`px-2 py-0.5 rounded-md text-[11px] font-bold ${st.tone}`}>{st.label}</span>
                            <span className="text-lg font-black tabular-nums w-10 text-right" aria-label="Điểm">{c.score ?? "—"}</span>
                            <Link to={`/progress-detail?milestoneId=${m.id}`} className="px-4 py-2 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest">
                                {c.submittedAt ? "Nộp lại" : "Nộp bài"}
                            </Link>
                        </div>
                    </li>
                );
            })}
            {row.weightedScore != null && (
                <li className="py-4 flex justify-between text-sm font-black">
                    <span>Điểm theo mốc (tạm tính){!row.complete && " — chưa chấm đủ"}</span>
                    <span className="tabular-nums">{row.weightedScore}</span>
                </li>
            )}
        </ul>
    );
}
