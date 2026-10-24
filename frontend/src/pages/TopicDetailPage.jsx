import { useCallback, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { topicApi } from "../api/topicApi";
import { registrationApi } from "../api/registrationApi";
import InlineNotice from "../components/InlineNotice";
import { ErrorState, LoadingState } from "../components/PageState";
import { useAuth } from "../context/AuthContext";
import useLoadEffect from "../hooks/useLoadEffect";
import { TOPIC_STATUS, WORKSPACE_STATUS, statusLabel } from "../constants/status";

const fmtDate = (v) => (v ? new Date(v).toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }) : "—");

/** "ThS. Nguyễn Văn A" — không lặp học vị khi họ tên đã có sẵn. */
const withDegree = ({ degree, fullName = "" }) =>
  degree && !fullName.toLowerCase().startsWith(degree.toLowerCase()) ? `${degree}. ${fullName}` : fullName;

const period = (w) => (w?.startAt || w?.endAt ? `${fmtDate(w.startAt)} – ${fmtDate(w.endAt)}` : "—");

const MY_STATUS = {
  CHO_DUYET: { text: "Đăng ký của bạn đang chờ duyệt", cls: "bg-tertiary-container text-on-tertiary-container", icon: "pending" },
  DA_DUYET: { text: "Bạn đã được duyệt vào đề tài này", cls: "bg-secondary-container text-on-secondary-container", icon: "verified" },
  TU_CHOI: { text: "Đăng ký của bạn đã bị từ chối", cls: "bg-error-container text-on-error-container", icon: "cancel" },
};

const card = "bg-white dark:bg-slate-900 rounded-[2rem] border border-outline-variant/10 shadow-sm";

/** P4-11 — chi tiết đề tài (mockup "chi tiết đề tài"): mô tả, giảng viên, chỉ tiêu, đăng ký, thành viên. */
export default function TopicDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const role = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const [topic, setTopic] = useState(null);
  const [error, setError] = useState("");
  const [registering, setRegistering] = useState(false);
  const [notice, setNotice] = useState(null);

  const load = useCallback(async () => {
    setError("");
    try {
      setTopic(await topicApi.getDetail(id));
    } catch (e) {
      setError(e.message || "Không tải được đề tài.");
    }
  }, [id]);
  useLoadEffect(load);

  const register = async () => {
    setRegistering(true);
    try {
      await registrationApi.registerTopic(user.id, topic.id);
      setNotice({ type: "success", message: "Đã gửi đăng ký. Giảng viên sẽ xem xét và phản hồi." });
      load();
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Đăng ký thất bại." });
    } finally {
      setRegistering(false);
    }
  };

  const back = (
    <button type="button" onClick={() => navigate(-1)}
      className="inline-flex items-center gap-1 text-sm font-bold text-outline hover:text-primary">
      <span className="material-symbols-outlined text-lg" aria-hidden="true">arrow_back</span>
      Quay lại
    </button>
  );

  if (error) return <div className="space-y-6">{back}<ErrorState message={error} onRetry={load} /></div>;
  if (!topic) return <LoadingState label="Đang tải đề tài..." />;

  const full = topic.approvedCount >= topic.capacity;
  const my = MY_STATUS[topic.myRegistrationStatus];
  const canRegister = topic.myRegistrationStatus === "CHUA_DANG_KY" && topic.status === "OPEN" && !full;
  const percent = Math.min(100, Math.round((topic.approvedCount / Math.max(1, topic.capacity)) * 100));

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      {back}
      {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} autoHideMs={4000} />}

      <div className="grid grid-cols-1 xl:grid-cols-[1fr_22rem] gap-6 items-start">
        <div className="space-y-6 min-w-0">
          <header>
            {topic.workspace?.name && (
              <span className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-black uppercase tracking-widest">
                {topic.workspace.name}
              </span>
            )}
            <h1 className="mt-3 text-3xl md:text-4xl font-black font-headline tracking-tight text-on-surface break-words">{topic.title}</h1>
          </header>

          <section className={`${card} p-6 md:p-8`} aria-labelledby="topic-desc">
            <h2 id="topic-desc" className="text-[11px] font-black uppercase tracking-widest text-primary mb-3">Mô tả đề tài</h2>
            {topic.description
              ? <p className="text-base leading-relaxed text-on-surface-variant whitespace-pre-line break-words">{topic.description}</p>
              : <p className="text-sm text-outline">Giảng viên chưa thêm mô tả.</p>}
          </section>

          <section className={`${card} p-6 md:p-8`} aria-labelledby="topic-info">
            <h2 id="topic-info" className="text-[11px] font-black uppercase tracking-widest text-primary mb-4">Thông tin đợt đồ án</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {[
                ["Đợt", topic.workspace?.name || "—"],
                ["Học kỳ", topic.workspace?.semester || "—"],
                ["Giai đoạn", statusLabel(WORKSPACE_STATUS, topic.workspace?.status) || "—"],
                ["Thời gian", period(topic.workspace)],
              ].map(([k, v]) => (
                <div key={k}>
                  <dt className="text-[10px] font-black uppercase tracking-widest text-outline">{k}</dt>
                  <dd className="mt-1 text-sm font-bold text-on-surface break-words">{v}</dd>
                </div>
              ))}
            </dl>
          </section>

          {topic.members && (
            <section className={`${card} p-6 md:p-8`} aria-labelledby="topic-members">
              <h2 id="topic-members" className="text-[11px] font-black uppercase tracking-widest text-primary mb-4">
                Sinh viên thực hiện ({topic.members.length})
              </h2>
              {topic.members.length === 0 ? (
                <p className="text-sm text-outline">Chưa có sinh viên nào được duyệt.</p>
              ) : (
                <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {topic.members.map((s) => (
                    <li key={s.id} className="flex items-center gap-3 rounded-2xl bg-surface-container-low p-4">
                      <span className="h-10 w-10 shrink-0 rounded-xl bg-primary text-on-primary flex items-center justify-center font-black" aria-hidden="true">
                        {s.user?.fullName?.charAt(0) || "S"}
                      </span>
                      <span className="min-w-0">
                        <span className="block text-sm font-bold truncate">{s.user?.fullName || "—"}</span>
                        <span className="block text-xs text-outline">MSSV {s.studentCode || "—"}{s.className ? ` · ${s.className}` : ""}</span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
              {topic.member && (
                <nav className="mt-5 flex flex-wrap gap-2" aria-label="Lối tắt của đề tài">
                  {[
                    ["/project-space", "tactic", "Không gian làm việc"],
                    ["/calendar", "calendar_month", "Lịch"],
                    ["/discussion", "forum", "Thảo luận"],
                  ].map(([to, icon, text]) => (
                    <Link key={to} to={to}
                      className="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-outline-variant/20 text-sm font-bold hover:bg-surface-container">
                      <span className="material-symbols-outlined text-lg" aria-hidden="true">{icon}</span>
                      {text}
                    </Link>
                  ))}
                </nav>
              )}
            </section>
          )}
        </div>

        <aside className={`${card} p-6 space-y-5 xl:sticky xl:top-24`} aria-label="Giảng viên và đăng ký">
          {topic.lecturer && (
            <div className="flex items-start gap-4 pb-5 border-b border-outline-variant/10">
              <span className="h-14 w-14 shrink-0 rounded-2xl bg-primary text-on-primary flex items-center justify-center text-xl font-black" aria-hidden="true">
                {topic.lecturer.fullName?.charAt(0) || "G"}
              </span>
              <div className="min-w-0">
                <p className="text-base font-black text-on-surface break-words">
                  {withDegree(topic.lecturer)}
                </p>
                {topic.lecturer.departmentName && <p className="text-sm text-on-surface-variant">{topic.lecturer.departmentName}</p>}
                {topic.lecturer.speciality && <p className="text-xs text-outline mt-0.5">{topic.lecturer.speciality}</p>}
                {topic.lecturer.email && (
                  <a href={`mailto:${topic.lecturer.email}`} className="mt-1 inline-flex items-center gap-1 text-xs font-bold text-primary hover:underline break-all">
                    <span className="material-symbols-outlined text-sm" aria-hidden="true">mail</span>
                    {topic.lecturer.email}
                  </a>
                )}
              </div>
            </div>
          )}

          <dl className="space-y-3 text-sm">
            <div className="flex justify-between gap-3">
              <dt className="text-on-surface-variant">Trạng thái</dt>
              <dd className="font-black text-right">{statusLabel(TOPIC_STATUS, topic.status)}</dd>
            </div>
            <div>
              <div className="flex justify-between gap-3">
                <dt className="text-on-surface-variant">Chỉ tiêu</dt>
                <dd className="font-black">{topic.approvedCount} / {topic.capacity} sinh viên</dd>
              </div>
              <div className="mt-2 h-1.5 rounded-full bg-surface-container overflow-hidden" role="progressbar"
                aria-valuenow={topic.approvedCount} aria-valuemin={0} aria-valuemax={topic.capacity} aria-label="Số sinh viên đã được duyệt">
                <div className="h-full bg-primary" style={{ width: `${percent}%` }}></div>
              </div>
            </div>
            {topic.owner && (
              <div className="flex justify-between gap-3">
                <dt className="text-on-surface-variant">Chờ duyệt</dt>
                <dd className="font-black">{topic.pendingCount}</dd>
              </div>
            )}
            <div className="flex justify-between gap-3">
              <dt className="text-on-surface-variant">Kết thúc đợt</dt>
              <dd className="font-black">{fmtDate(topic.workspace?.endAt)}</dd>
            </div>
          </dl>

          {my && (
            <p className={`flex items-center gap-2 rounded-xl px-4 py-3 text-sm font-bold ${my.cls}`}>
              <span className="material-symbols-outlined text-lg" aria-hidden="true">{my.icon}</span>
              {my.text}
            </p>
          )}
          {canRegister && (
            <button type="button" onClick={register} disabled={registering}
              className="w-full inline-flex items-center justify-center gap-2 px-5 py-3.5 rounded-2xl bg-primary text-on-primary font-bold shadow-lg shadow-primary/20 disabled:opacity-50">
              <span className="material-symbols-outlined text-lg" aria-hidden="true">how_to_reg</span>
              {registering ? "Đang gửi..." : "Gửi yêu cầu đăng ký"}
            </button>
          )}
          {topic.myRegistrationStatus === "CHUA_DANG_KY" && !canRegister && (
            <p className="text-xs text-outline text-center">Đề tài hiện không nhận đăng ký.</p>
          )}
          {topic.owner && role === "LECTURER" && (
            <Link to="/lecturer" className="block text-center text-sm font-bold text-primary hover:underline">Xem và duyệt đăng ký</Link>
          )}
        </aside>
      </div>
    </div>
  );
}
