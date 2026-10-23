import { useCallback, useState } from "react";
import { milestoneApi } from "../../api/milestoneApi";
import InlineNotice from "../../components/InlineNotice";
import useLoadEffect from "../../hooks/useLoadEffect";
import MilestoneForm from "./MilestoneForm";
import MilestoneGradeGrid from "./MilestoneGradeGrid";
import StudentMilestoneList from "./StudentMilestoneList";

/**
 * P4-05 — mốc của đề tài: giảng viên tạo/sửa/xóa mốc (có trọng số) và chấm từng mốc; sinh viên xem hạn, trạng thái
 * nộp (đúng hạn / trễ), điểm và nộp bài.
 */
export default function MilestonePanel({ topicId, isLecturer }) {
    const [board, setBoard] = useState(null);
    const [notice, setNotice] = useState(null);
    const [busy, setBusy] = useState(false);
    const [editing, setEditing] = useState(null); // null | "new" | milestone

    const load = useCallback(async () => {
        if (!topicId) return;
        try {
            setBoard(await milestoneApi.board(topicId));
        } catch (e) {
            setNotice({ type: "danger", message: e.message || "Không tải được các mốc." });
        }
    }, [topicId]);
    useLoadEffect(load);

    const act = async (fn, success) => {
        setBusy(true);
        try {
            await fn();
            if (success) setNotice({ type: "success", message: success });
            await load();
            return true;
        } catch (e) {
            setNotice({ type: "danger", message: e.message || "Thao tác thất bại." });
            return false;
        } finally {
            setBusy(false);
        }
    };

    const saveMilestone = async (payload) => {
        const ok = editing === "new"
            ? await act(() => milestoneApi.create({ ...payload, topicId: Number(topicId) }), "Đã thêm mốc.")
            : await act(() => milestoneApi.update(editing.id, payload), "Đã lưu mốc.");
        if (ok) setEditing(null);
    };
    const deleteMilestone = (m) => {
        if (window.confirm(`Xóa mốc "${m.title}"?`)) act(() => milestoneApi.delete(m.id), "Đã xóa mốc.");
    };
    const grade = (g) => act(() => milestoneApi.grade(g.milestoneId, g.studentId, { score: Number(g.score), feedback: g.feedback }), "Đã lưu điểm.");

    if (!topicId || !board) return null;
    const weightWarning = isLecturer && board.weightTotal > 0 && board.weightTotal !== 100;

    return (
        <section className="bg-white dark:bg-slate-900 p-6 md:p-8 rounded-[2rem] border border-outline-variant/10 shadow-sm space-y-5" aria-label="Mốc và chấm điểm">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h2 className="text-sm font-black uppercase tracking-[0.2em] text-on-surface">Mốc tiến độ{isLecturer ? " & chấm điểm" : ""}</h2>
                    <p className="text-xs text-outline font-bold mt-1">
                        {isLecturer
                            ? "Mỗi mốc có hạn nộp và trọng số; điểm đề xuất = trung bình có trọng số của các mốc đã chấm. Điểm tổng kết vẫn nhập ở trang Duyệt đăng ký."
                            : "Nộp bài cho từng mốc trước hạn chót; điểm và nhận xét của giảng viên hiện tại đây."}
                    </p>
                </div>
                {isLecturer && editing !== "new" && (
                    <button type="button" onClick={() => setEditing("new")} className="px-5 py-2.5 rounded-xl bg-primary text-white text-[10px] font-black uppercase tracking-widest shadow-lg shadow-primary/20">
                        Thêm mốc
                    </button>
                )}
            </div>
            {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} autoHideMs={notice.type === "success" ? 3000 : undefined} />}
            {weightWarning && <p className="text-xs font-bold text-tertiary">Tổng trọng số các mốc hiện là {board.weightTotal}% (nên bằng 100%).</p>}
            {isLecturer && editing && (
                <MilestoneForm key={editing === "new" ? "new" : editing.id} initial={editing === "new" ? null : editing}
                    onSubmit={saveMilestone} onCancel={() => setEditing(null)} busy={busy} />
            )}
            {board.milestones.length === 0 ? (
                <p className="text-xs font-bold text-outline">Đề tài chưa có mốc nào{isLecturer ? " — bấm \"Thêm mốc\" để tạo." : "."}</p>
            ) : isLecturer ? (
                <MilestoneGradeGrid board={board} onGrade={grade} onEdit={setEditing} onDelete={deleteMilestone} busy={busy} />
            ) : (
                <StudentMilestoneList board={board} />
            )}
        </section>
    );
}
