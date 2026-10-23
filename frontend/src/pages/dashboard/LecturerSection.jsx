import { Link } from "react-router-dom";
import { StatTile, fmtDate, panel, panelTitle } from "./charts";

/** P4-01 — việc cần làm của giảng viên: đăng ký chờ duyệt, báo cáo chưa nhận xét, SV "im lặng", mốc sắp đến hạn. */
export default function LecturerSection({ stats }) {
    const silent = stats.silentStudents || [];
    const milestones = stats.upcomingMilestones || [];
    return (
        <section className="space-y-6" aria-label="Việc cần làm">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatTile icon="menu_book" label="Đề tài của tôi" value={stats.myTopics ?? 0} hint="Đang phụ trách" />
                <StatTile icon="group" label="Sinh viên hướng dẫn" value={stats.approvedStudents ?? 0} hint="Đăng ký đã được duyệt" />
                <StatTile icon="pending_actions" label="Đăng ký chờ duyệt" value={stats.pendingRegistrations ?? 0} hint="Cần duyệt hoặc từ chối" />
                <StatTile icon="rate_review" label="Báo cáo chưa nhận xét" value={stats.uncommentedReports ?? 0} hint="Báo cáo tiến độ của sinh viên" />
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <section className={panel} aria-label="Sinh viên chưa nộp tiến độ">
                    <h3 className={panelTitle}>Sinh viên chưa nộp tiến độ hơn 7 ngày</h3>
                    {silent.length === 0 ? <p className="text-xs font-bold text-outline">Không có — mọi sinh viên đều nộp đều đặn.</p> : (
                        <ul className="divide-y divide-outline-variant/10">
                            {silent.map((s) => (
                                <li key={`${s.topicId}-${s.studentCode}`} className="py-3 flex justify-between gap-3 text-sm">
                                    <span>
                                        <span className="font-bold">{s.studentName}</span> <span className="text-outline text-xs">{s.studentCode}</span>
                                        <span className="block text-xs text-outline">{s.topicTitle}</span>
                                    </span>
                                    <span className="text-xs font-bold text-on-surface-variant shrink-0 text-right">
                                        {s.lastReportAt ? `Lần cuối ${fmtDate(s.lastReportAt)}` : "Chưa nộp lần nào"}
                                    </span>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
                <section className={panel} aria-label="Mốc sắp đến hạn">
                    <h3 className={panelTitle}>Mốc sắp đến hạn (14 ngày)</h3>
                    {milestones.length === 0 ? <p className="text-xs font-bold text-outline">Không có mốc nào sắp đến hạn.</p> : (
                        <ul className="divide-y divide-outline-variant/10">
                            {milestones.map((m) => (
                                <li key={m.id} className="py-3 flex justify-between gap-3 text-sm">
                                    <span><span className="font-bold">{m.title}</span><span className="block text-xs text-outline">{m.topicTitle}</span></span>
                                    <span className="text-xs font-bold text-on-surface-variant shrink-0">{fmtDate(m.deadline)}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            </div>
            <div className="flex flex-wrap gap-3">
                <Link to="/lecturer" className="px-6 py-2.5 bg-primary text-white rounded-xl font-black text-[10px] uppercase tracking-widest shadow-lg shadow-primary/20">Duyệt đăng ký</Link>
                <Link to="/lecturer-progress" className="px-6 py-2.5 bg-surface-container text-on-surface-variant rounded-xl font-black text-[10px] uppercase tracking-widest">Nhận xét tiến độ</Link>
            </div>
        </section>
    );
}
