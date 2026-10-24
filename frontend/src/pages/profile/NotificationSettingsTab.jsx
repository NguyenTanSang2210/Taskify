import { useCallback, useState } from "react";
import { notificationsApi } from "../../api/notificationsApi";
import InlineNotice from "../../components/InlineNotice";
import useLoadEffect from "../../hooks/useLoadEffect";

const CATEGORIES = [
    { key: "registration", label: "Kết quả đăng ký đề tài", hint: "Đăng ký được duyệt hoặc bị từ chối" },
    { key: "progress", label: "Báo cáo tiến độ & nhận xét", hint: "Sinh viên nộp báo cáo, giảng viên nhận xét" },
    { key: "grade", label: "Điểm & chấm lại", hint: "Có điểm, sửa điểm, yêu cầu chấm lại" },
    { key: "reminder", label: "Nhắc hạn tự động", hint: "Mốc sắp đến hạn, lâu chưa nộp tiến độ" },
];

/** P4-06 — chọn nhóm email muốn nhận và gửi ngay hay gom thành một thư mỗi sáng. Thông báo trong ứng dụng không đổi. */
export default function NotificationSettingsTab() {
    const [settings, setSettings] = useState(null);
    const [saving, setSaving] = useState(false);
    const [notice, setNotice] = useState(null);

    const load = useCallback(async () => {
        try {
            setSettings(await notificationsApi.getSettings());
        } catch (e) {
            setNotice({ type: "danger", message: e.message || "Không tải được cài đặt thông báo." });
        }
    }, []);
    useLoadEffect(load);

    const save = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            const { pendingDigest: _ignored, ...body } = settings;
            setSettings(await notificationsApi.saveSettings(body));
            setNotice({ type: "success", message: "Đã lưu cài đặt thông báo." });
        } catch (err) {
            setNotice({ type: "danger", message: err.message || "Lưu thất bại." });
        } finally {
            setSaving(false);
        }
    };

    if (!settings) return notice ? <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} /> : null;
    const toggle = (key) => setSettings((s) => ({ ...s, [key]: !s[key] }));

    return (
        <form onSubmit={save} className="space-y-8">
            <div>
                <h3 className="text-xl font-black text-on-surface uppercase tracking-tight font-headline">Thông báo qua email</h3>
                <p className="text-[11px] text-outline font-bold mt-1">
                    Thông báo trong ứng dụng (chuông) luôn được giữ. Email bảo mật — mã OTP, kích hoạt, đặt lại mật khẩu — luôn được gửi ngay.
                </p>
            </div>
            {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} autoHideMs={notice.type === "success" ? 3000 : undefined} />}

            <fieldset className="space-y-3">
                <legend className="text-[10px] font-black uppercase tracking-[0.2em] text-outline mb-3">Nhóm email muốn nhận</legend>
                {CATEGORIES.map((c) => (
                    <label key={c.key} className="flex items-start gap-4 p-4 rounded-2xl bg-surface-container-low cursor-pointer">
                        <input type="checkbox" checked={!!settings[c.key]} onChange={() => toggle(c.key)} className="mt-1 h-4 w-4 accent-primary" />
                        <span>
                            <span className="block text-sm font-bold text-on-surface">{c.label}</span>
                            <span className="block text-xs text-outline">{c.hint}</span>
                        </span>
                    </label>
                ))}
            </fieldset>

            <fieldset className="space-y-3">
                <legend className="text-[10px] font-black uppercase tracking-[0.2em] text-outline mb-3">Cách gửi</legend>
                {[{ value: false, label: "Gửi ngay", hint: "Mỗi sự kiện một email" },
                  { value: true, label: "Tổng hợp mỗi sáng (07:30)", hint: "Gom mọi thông báo trong ngày thành một email" }].map((o) => (
                    <label key={String(o.value)} className="flex items-start gap-4 p-4 rounded-2xl bg-surface-container-low cursor-pointer">
                        <input type="radio" name="emailDigest" checked={settings.emailDigest === o.value}
                            onChange={() => setSettings((s) => ({ ...s, emailDigest: o.value }))} className="mt-1 h-4 w-4 accent-primary" />
                        <span>
                            <span className="block text-sm font-bold text-on-surface">{o.label}</span>
                            <span className="block text-xs text-outline">{o.hint}</span>
                        </span>
                    </label>
                ))}
                {settings.pendingDigest > 0 && (
                    <p className="text-xs font-bold text-on-surface-variant">{settings.pendingDigest} thông báo đang chờ trong thư tổng hợp sáng mai.</p>
                )}
            </fieldset>

            <button type="submit" disabled={saving} className="px-10 py-4 bg-primary text-on-primary rounded-2xl font-black text-xs uppercase tracking-widest shadow-xl shadow-primary/20 disabled:opacity-50">
                {saving ? "Đang lưu..." : "Lưu cài đặt"}
            </button>
        </form>
    );
}
