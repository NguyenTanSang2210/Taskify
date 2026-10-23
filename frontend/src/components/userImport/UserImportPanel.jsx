import { useRef, useState } from "react";
import { importApi } from "../../api/importApi";
import { saveBlob } from "../../api/download";
import InlineNotice from "../InlineNotice";
import ImportPreviewTable from "./ImportPreviewTable";
import { errorsCsv } from "./errorsCsv";

const KINDS = [
    { id: "STUDENT", label: "Sinh viên", icon: "school" },
    { id: "LECTURER", label: "Giảng viên", icon: "co_present" },
];

const label = "block text-[10px] font-black uppercase tracking-[0.2em] text-outline mb-2";
const button = "inline-flex items-center gap-2 px-6 py-3 rounded-2xl text-[10px] font-black uppercase tracking-widest transition-all disabled:opacity-40 disabled:cursor-not-allowed";

/**
 * P4-03 — nhập hàng loạt tài khoản từ Excel: tải file mẫu → chọn file → kiểm tra (báo lỗi từng dòng) → nhập các dòng
 * hợp lệ. {@code departments} chỉ truyền cho ADMIN (chọn khoa); quản trị khoa luôn nhập vào khoa của mình.
 */
export default function UserImportPanel({ departments = null }) {
    const [kind, setKind] = useState("STUDENT");
    const [departmentId, setDepartmentId] = useState("");
    const [createMissingClasses, setCreateMissingClasses] = useState(false);
    const [file, setFile] = useState(null);
    const [report, setReport] = useState(null);
    const [onlyErrors, setOnlyErrors] = useState(false);
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState(null);
    const fileInput = useRef(null);

    const chooseAdminDepartment = departments !== null;
    const needsDepartment = chooseAdminDepartment && !departmentId;
    const input = { file, kind, departmentId: chooseAdminDepartment ? departmentId : null, createMissingClasses };

    // Đổi bất kỳ tham số nào → kết quả kiểm tra cũ không còn đúng
    const resetWith = (setter) => (value) => {
        setter(value);
        setReport(null);
        setNotice(null);
    };
    const changeKind = resetWith(setKind);
    const changeDepartment = resetWith(setDepartmentId);
    const changeCreateClasses = resetWith(setCreateMissingClasses);
    const changeFile = resetWith(setFile);

    const run = async (action, onDone) => {
        setBusy(true);
        setNotice(null);
        try {
            onDone(await action());
        } catch (e) {
            setNotice({ type: "danger", message: e.message || "Không xử lý được file." });
        } finally {
            setBusy(false);
        }
    };

    const downloadTemplate = () => run(() => importApi.downloadTemplate(kind), () => {});

    const check = () => run(() => importApi.preview(input), (r) => {
        setReport(r);
        setOnlyErrors(r.invalid > 0);
    });

    const commit = () => {
        const skipped = report.invalid ? ` (bỏ qua ${report.invalid} dòng lỗi)` : "";
        if (!window.confirm(`Tạo ${report.valid} tài khoản${skipped} cho khoa ${report.departmentName}?`)) return;
        run(() => importApi.commit(input), (r) => {
            setReport(null);
            setFile(null);
            if (fileInput.current) fileInput.current.value = "";
            const classes = r.newClasses?.length ? ` Đã tạo thêm lớp: ${r.newClasses.join(", ")}.` : "";
            setNotice({
                type: "success",
                message: `Đã tạo ${r.created} tài khoản cho khoa ${r.departmentName}${r.invalid ? `, bỏ qua ${r.invalid} dòng lỗi` : ""}.`
                    + `${classes} Mã kích hoạt đang được gửi tới email của từng người.`,
            });
        });
    };

    const downloadErrors = () => {
        const blob = new Blob([errorsCsv(report, kind)], { type: "text/csv;charset=utf-8" });
        saveBlob(blob, kind === "LECTURER" ? "loi-nhap-giang-vien.csv" : "loi-nhap-sinh-vien.csv");
    };

    return (
        <div className="p-8 md:p-10 space-y-8">
            <div>
                <h3 className="text-sm font-black text-outline uppercase tracking-[0.2em]">Nhập tài khoản từ Excel</h3>
                <p className="mt-2 text-xs font-bold text-on-surface-variant leading-relaxed max-w-3xl">
                    Tài khoản được tạo ở trạng thái <b>chưa kích hoạt</b>; mỗi người nhận mã kích hoạt qua email và tự đặt mật khẩu.
                    Hệ thống kiểm tra toàn bộ file và báo lỗi từng dòng trước khi nhập — chỉ các dòng hợp lệ được tạo.
                </p>
            </div>

            {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} />}

            <div className="grid gap-6 md:grid-cols-3">
                <fieldset>
                    <legend className={label}>Loại tài khoản</legend>
                    <div className="flex gap-2">
                        {KINDS.map((k) => (
                            <button key={k.id} type="button" onClick={() => changeKind(k.id)} aria-pressed={kind === k.id}
                                className={`${button} ${kind === k.id ? "bg-primary text-white shadow-lg shadow-primary/20" : "bg-surface-container-low text-outline hover:text-on-surface"}`}>
                                <span className="material-symbols-outlined text-lg" aria-hidden="true">{k.icon}</span>
                                {k.label}
                            </button>
                        ))}
                    </div>
                </fieldset>

                {chooseAdminDepartment && (
                    <div>
                        <label htmlFor="import-department" className={label}>Khoa</label>
                        <select id="import-department" value={departmentId} onChange={(e) => changeDepartment(e.target.value)}
                            className="w-full px-4 py-3 bg-surface-container-low rounded-2xl text-sm font-bold outline-none focus:ring-2 focus:ring-primary/30">
                            <option value="">— Chọn khoa —</option>
                            {departments.map((d) => <option key={d.id} value={String(d.id)}>{d.code} - {d.name}</option>)}
                        </select>
                    </div>
                )}

                {kind === "STUDENT" && (
                    <label className="flex items-start gap-3 cursor-pointer md:pt-7">
                        <input type="checkbox" checked={createMissingClasses} onChange={(e) => changeCreateClasses(e.target.checked)}
                            className="mt-0.5 h-4 w-4 accent-primary" />
                        <span className="text-xs font-bold text-on-surface-variant">
                            Tự tạo lớp chưa có trong khoa
                            <span className="block text-[11px] font-medium text-outline">Không chọn: dòng có mã lớp lạ bị báo lỗi.</span>
                        </span>
                    </label>
                )}
            </div>

            <div className="flex flex-wrap items-end gap-4">
                <div className="grow min-w-[16rem]">
                    <label htmlFor="import-file" className={label}>File Excel (.xlsx, tối đa 2000 dòng)</label>
                    <input id="import-file" ref={fileInput} type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        onChange={(e) => changeFile(e.target.files?.[0] || null)}
                        className="block w-full text-sm file:mr-4 file:px-5 file:py-2.5 file:rounded-xl file:border-0 file:bg-surface-container file:text-xs file:font-black file:uppercase file:tracking-widest" />
                </div>
                <button type="button" onClick={downloadTemplate} disabled={busy} className={`${button} bg-surface-container-low text-on-surface hover:bg-surface-container`}>
                    <span className="material-symbols-outlined text-lg" aria-hidden="true">download</span>
                    Tải file mẫu
                </button>
                <button type="button" onClick={check} disabled={busy || !file || needsDepartment} className={`${button} bg-primary text-white shadow-lg shadow-primary/20`}>
                    <span className="material-symbols-outlined text-lg" aria-hidden="true">fact_check</span>
                    {busy && !report ? "Đang kiểm tra..." : "Kiểm tra file"}
                </button>
            </div>
            {needsDepartment && file && <p className="text-xs font-bold text-amber-700">Chọn khoa trước khi kiểm tra file.</p>}

            {report && (
                <section aria-label="Kết quả kiểm tra" className="space-y-5">
                    <div className="flex flex-wrap items-center gap-3">
                        <span className="px-4 py-2 rounded-xl bg-surface-container-low text-xs font-black">Khoa: {report.departmentName}</span>
                        <span className="px-4 py-2 rounded-xl bg-surface-container-low text-xs font-black">Tổng: {report.total}</span>
                        <span className="px-4 py-2 rounded-xl bg-green-500/10 text-green-700 text-xs font-black">Hợp lệ: {report.valid}</span>
                        <span className={`px-4 py-2 rounded-xl text-xs font-black ${report.invalid ? "bg-error/10 text-error" : "bg-surface-container-low text-outline"}`}>Lỗi: {report.invalid}</span>
                        {report.newClasses?.length > 0 && (
                            <span className="px-4 py-2 rounded-xl bg-amber-500/10 text-amber-700 text-xs font-black">Lớp sẽ tạo mới: {report.newClasses.join(", ")}</span>
                        )}
                    </div>

                    <div className="flex flex-wrap items-center gap-3">
                        <button type="button" onClick={commit} disabled={busy || report.valid === 0} className={`${button} bg-primary text-white shadow-lg shadow-primary/20`}>
                            <span className="material-symbols-outlined text-lg" aria-hidden="true">group_add</span>
                            {busy ? "Đang nhập..." : `Nhập ${report.valid} tài khoản hợp lệ`}
                        </button>
                        {report.invalid > 0 && (
                            <>
                                <button type="button" onClick={downloadErrors} className={`${button} bg-surface-container-low text-on-surface hover:bg-surface-container`}>
                                    <span className="material-symbols-outlined text-lg" aria-hidden="true">file_download</span>
                                    Tải danh sách lỗi (CSV)
                                </button>
                                <label className="flex items-center gap-2 text-xs font-bold text-on-surface-variant cursor-pointer">
                                    <input type="checkbox" checked={onlyErrors} onChange={(e) => setOnlyErrors(e.target.checked)} className="h-4 w-4 accent-primary" />
                                    Chỉ hiện dòng lỗi
                                </label>
                            </>
                        )}
                    </div>

                    <ImportPreviewTable report={report} kind={kind} onlyErrors={onlyErrors} />
                </section>
            )}
        </div>
    );
}
