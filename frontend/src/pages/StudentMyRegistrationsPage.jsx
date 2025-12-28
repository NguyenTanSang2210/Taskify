import React, { useEffect, useState } from "react";
import { registrationApi } from "../api/registrationApi";
import InlineNotice from "../components/InlineNotice";
import { useAuth } from "../context/AuthContext";

export default function StudentMyRegistrationsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const { user } = useAuth();
  const roleName = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isStudent = roleName === "STUDENT";
  const studentId = isStudent ? user?.id : null;
  const [notice, setNotice] = useState(null);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      if (!isStudent || !studentId) {
        setItems([]);
        return;
      }
      const data = await registrationApi.getMine(studentId);
      setItems(data || []);
    } catch (e) {
      console.error(e);
      setError("Không tải được đăng ký của bạn");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studentId]);

  const handleCancel = async (regId) => {
    if (!window.confirm("Bạn có chắc muốn hủy đăng ký này?")) return;
    try {
      await registrationApi.cancel(regId);
      await load();
      setNotice({ type: "success", message: "Đã hủy đăng ký." });
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Không thể hủy đăng ký (có thể đã được duyệt)." });
      setTimeout(() => setNotice(null), 3000);
    }
  };

  const renderStatus = (approved) => {
    if (approved === null) return <span className="badge bg-secondary">Chờ duyệt</span>;
    if (approved === true) return <span className="badge bg-success">Đã duyệt</span>;
    return <span className="badge bg-danger">Bị từ chối</span>;
  };

  return (
    <div className="container py-4">
      <h3>Đăng ký của tôi</h3>
      {notice && (
        <InlineNotice
          type={notice.type}
          message={notice.message}
          onClose={() => setNotice(null)}
        />
      )}
      {!user && (
        <p className="text-muted mt-2">Bạn chưa đăng nhập. Vui lòng đăng nhập bằng tài khoản Sinh viên.</p>
      )}
      {loading && <p className="text-muted mt-3">Đang tải...</p>}
      {error && !loading && <p className="text-danger mt-3">{error}</p>}
      {!loading && !error && items.length === 0 && (
        <p className="text-muted mt-3">Bạn chưa có đăng ký nào.</p>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="table-responsive mt-3">
          <table className="table table-striped">
            <thead>
              <tr>
                <th>Đề tài</th>
                <th>Giảng viên</th>
                <th>Thời gian đăng ký</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((r) => (
                <tr key={r.id}>
                  <td>{r.topic?.title}</td>
                  <td>{r.topic?.lecturer?.fullName}</td>
                  <td>{new Date(r.registeredAt).toLocaleString()}</td>
                  <td>
                    {renderStatus(r.approved)}
                    {r.approved === false && r.rejectReason && (
                      <div className="small text-muted mt-1">Lý do: {r.rejectReason}</div>
                    )}
                  </td>
                  <td className="text-end">
                    {r.approved === null && (
                      <button className="btn btn-outline-danger btn-sm" onClick={() => handleCancel(r.id)}>
                        Hủy đăng ký
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
