// src/pages/LecturerRegistrationsPage.jsx
import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { registrationApi } from "../api/registrationApi";
import RegistrationTable from "../components/RegistrationTable";
import InlineNotice from "../components/InlineNotice";
import { connectAndSubscribe } from "../ws/stomp";

export default function LecturerRegistrationPage() {
  const location = useLocation();
  const [topicId, setTopicId] = useState("1");
  const [data, setData] = useState([]);
  const [loadingId, setLoadingId] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState("registered_desc");

  // state cho modal
  const [showModal, setShowModal] = useState(false);
  const [modalAction, setModalAction] = useState(null); // "approve" | "reject" | "grade"
  const [selectedReg, setSelectedReg] = useState(null);
  const [notice, setNotice] = useState(null);
  
  const [gradeScore, setGradeScore] = useState(0);
  const [gradeFeedback, setGradeFeedback] = useState("");

  const load = React.useCallback(async () => {
    const result = await registrationApi.getByTopic(topicId);
    setData(result);
  }, [topicId]);

  // Đọc topicId từ query string khi mở trang hoặc khi thay đổi URL
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const qId = params.get("topicId");
    if (qId) {
      setTopicId(qId);
    }
  }, [location.search]);

  useEffect(() => {
    if (!topicId) return;
    const client = connectAndSubscribe({
      destination: `/topic/registration/${topicId}`,
      onMessage: async (payload) => {
        const type = payload?.type;
        const reg = payload?.registration;
        if (!type || !reg) return;
        await load();
        setNotice({ type: "info", message: "Có cập nhật đăng ký realtime" });
        setTimeout(() => setNotice(null), 2000);
      },
    });
    return () => client?.deactivate?.();
  }, [topicId, load]);

  // mở modal duyệt
  const openApproveModal = (reg) => {
    setSelectedReg(reg);
    setModalAction("approve");
    setShowModal(true);
  };

  // mở modal từ chối
  const openRejectModal = (reg) => {
    setSelectedReg(reg);
    setModalAction("reject");
    setShowModal(true);
  };

  // mở modal chấm điểm
  const openGradeModal = (reg) => {
    setSelectedReg(reg);
    setGradeScore(reg.score || 0);
    setGradeFeedback(reg.feedback || "");
    setModalAction("grade");
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedReg(null);
    setModalAction(null);
  };

  const handleConfirm = async () => {
    if (!selectedReg || !modalAction) return;

    setLoadingId(selectedReg.id);

    try {
      if (modalAction === "approve") {
        await registrationApi.approve(selectedReg.id);
      } else if (modalAction === "reject") {
        await registrationApi.reject(selectedReg.id, rejectReason);
      } else if (modalAction === "grade") {
        await registrationApi.grade(selectedReg.id, gradeScore, gradeFeedback);
      }

      await load(); // reload danh sách
      setNotice({ type: "success", message: "Đã cập nhật thành công" });
      setTimeout(() => setNotice(null), 2500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Có lỗi xảy ra. Vui lòng thử lại." });
      setTimeout(() => setNotice(null), 3000);
    } finally {
      setLoadingId(null);
      closeModal();
    }
  };

  // Dữ liệu sau khi lọc và sắp xếp
  const filteredData = React.useMemo(() => {
    const q = search.trim().toLowerCase();
    let arr = data || [];
    if (q) {
      arr = arr.filter((r) => {
        const student = r.student || {};
        const user = student.user || {};
        return (
          String(r.id).includes(q) ||
          (student.studentCode || "").toLowerCase().includes(q) ||
          (student.className || "").toLowerCase().includes(q) ||
          (user.fullName || "").toLowerCase().includes(q)
        );
      });
    }

    const cmp = {
      registered_desc: (a, b) => new Date(b.registeredAt) - new Date(a.registeredAt),
      registered_asc: (a, b) => new Date(a.registeredAt) - new Date(b.registeredAt),
      studentCode_asc: (a, b) => String(a.student?.studentCode || "").localeCompare(String(b.student?.studentCode || "")),
      fullName_asc: (a, b) => String(a.student?.user?.fullName || "").localeCompare(String(b.student?.user?.fullName || "")),
    };
    const sorter = cmp[sortBy] || cmp.registered_desc;
    return [...arr].sort(sorter);
  }, [data, search, sortBy]);

  // Tiêu đề đề tài và thống kê nhanh
  const topicTitle = React.useMemo(() => {
    const t = data?.[0]?.topic;
    return t?.title ? t.title : `Đề tài #${topicId}`;
  }, [data, topicId]);
  const stats = React.useMemo(() => {
    const total = data.length;
    let pending = 0, approved = 0, rejected = 0;
    data.forEach((r) => {
      if (r.approved === null) pending++;
      else if (r.approved === true) approved++;
      else rejected++;
    });
    return { total, pending, approved, rejected };
  }, [data]);

  const exportCsv = () => {
    const rows = [
      [
        "Mã đăng ký",
        "MSSV",
        "Lớp",
        "Họ tên",
        "Ngày đăng ký",
        "Người xử lý",
        "Thời gian xử lý",
        "Trạng thái",
        "Điểm",
        "Lý do từ chối",
      ],
      ...filteredData.map((r) => {
        const student = r.student || {};
        const user = student.user || {};
        const stt = r.approved === null ? "CHUA_DUYET" : r.approved ? "DA_DUYET" : "TU_CHOI";
        const dateStr = new Date(r.registeredAt).toLocaleString();
        const reviewerName = r.reviewer?.user?.fullName || "";
        const reviewedStr = r.reviewedAt ? new Date(r.reviewedAt).toLocaleString() : "";
        return [
          r.id,
          student.studentCode || "",
          student.className || "",
          user.fullName || "",
          dateStr,
          reviewerName,
          reviewedStr,
          stt,
          r.score !== null ? r.score : "",
          r.rejectReason || "",
        ];
      }),
    ];

    const csv = rows
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(","))
      .join("\r\n");
    const blob = new Blob(["\ufeff" + csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `dang-ky-topic-${topicId}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const handleExportExcel = async () => {
      try {
          const blob = await registrationApi.exportExcel(topicId);
          const url = window.URL.createObjectURL(new Blob([blob]));
          const link = document.createElement('a');
          link.href = url;
          link.setAttribute('download', `dang-ky-de-tai-${topicId}.xlsx`);
          document.body.appendChild(link);
          link.click();
          link.parentNode.removeChild(link);
      } catch (error) {
          console.error("Xuất tệp thất bại", error);
          setNotice({ type: "danger", message: "Xuất Excel thất bại" });
          setTimeout(() => setNotice(null), 3000);
      }
  };

  return (
    <div className="container py-4">
      <h3>Giảng viên duyệt đăng ký đề tài</h3>
      {notice && (
        <InlineNotice
          type={notice.type}
          message={notice.message}
          onClose={() => setNotice(null)}
        />
      )}
      <div className="mt-2">
        <strong>Đề tài:</strong> {topicTitle}
        <span className="ms-3 badge bg-dark" title="Tổng">{stats.total}</span>
        <span className="ms-1 badge bg-warning" title="Chờ">{stats.pending}</span>
        <span className="ms-1 badge bg-success" title="Duyệt">{stats.approved}</span>
        <span className="ms-1 badge bg-danger" title="Từ chối">{stats.rejected}</span>
      </div>

      <div className="row mb-3 mt-3">
        <div className="col-md-3">
          <input
            className="form-control"
            value={topicId}
            onChange={(e) => setTopicId(e.target.value)}
          />
        </div>
        <div className="col-md-2">
          <button className="btn btn-primary w-100" onClick={load}>
            Tải danh sách
          </button>
        </div>
        <div className="col-md-2">
          <button className="btn btn-success w-100" onClick={handleExportExcel}>
            <i className="bi bi-file-earmark-excel me-1"></i>Xuất Excel
          </button>
        </div>
        <div className="col-md-2">
          <button className="btn btn-outline-secondary w-100" onClick={exportCsv}>
            <i className="bi bi-filetype-csv me-1"></i>Xuất CSV
          </button>
        </div>
        <div className="col-md-3">
          <input
            className="form-control"
            placeholder="Tìm theo MSSV, Họ tên, Lớp..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="col-md-2">
          <select
            className="form-select"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
          >
            <option value="registered_desc">Mới nhất</option>
            <option value="registered_asc">Cũ nhất</option>
            <option value="studentCode_asc">MSSV A-Z</option>
            <option value="fullName_asc">Tên A-Z</option>
          </select>
        </div>
      </div>

      <RegistrationTable
        data={filteredData}
        loadingId={loadingId}
        onApproveClick={openApproveModal}
        onRejectClick={openRejectModal}
        onGradeClick={openGradeModal}
      />

      {/* Modal xác nhận đơn giản bằng React + CSS */}
      {showModal && selectedReg && (
        <div className="custom-modal-backdrop">
          <div className="custom-modal">
            <h5 className="mb-3">
              {modalAction === "approve"
                ? "Xác nhận duyệt"
                : modalAction === "reject"
                ? "Xác nhận từ chối"
                : "Chấm điểm & Tổng kết"}
            </h5>

            <p className="mb-1">
              Sinh viên: <strong>{selectedReg.student?.user?.fullName}</strong>
            </p>
            <p className="mb-1">
              MSSV: <strong>{selectedReg.student?.studentCode}</strong>
            </p>
            
            {modalAction !== "grade" && (
                <p className="mb-3">
                Bạn có chắc muốn{" "}
                <strong>{modalAction === "approve" ? "DUYỆT" : "TỪ CHỐI"}</strong> đăng ký này không?
                </p>
            )}

            {modalAction === "reject" && (
              <div className="mb-3">
                <label className="form-label">Lý do từ chối</label>
                <textarea
                  className="form-control"
                  rows={3}
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Nhập lý do từ chối (tùy chọn)"
                />
              </div>
            )}

            {modalAction === "grade" && (
              <div className="mb-3">
                <div className="mb-2">
                    <label className="form-label">Điểm số (0 - 10)</label>
                    <input
                      type="number"
                      step="0.1"
                      min="0"
                      max="10"
                      className="form-control"
                      value={gradeScore}
                      onChange={(e) => setGradeScore(e.target.value)}
                    />
                </div>
                <div className="mb-2">
                    <label className="form-label">Nhận xét / Đánh giá</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      value={gradeFeedback}
                      onChange={(e) => setGradeFeedback(e.target.value)}
                      placeholder="Nhập nhận xét..."
                    />
                </div>
                <div className="alert alert-warning small">
                    Lưu ý: Sau khi chấm điểm, trạng thái đề tài sẽ chuyển thành <strong>HOÀN THÀNH</strong>.
                </div>
              </div>
            )}

            <div className="d-flex justify-content-end gap-2 mt-4">
              <button
                className="btn btn-secondary"
                onClick={closeModal}
                disabled={!!loadingId}
              >
                Hủy bỏ
              </button>
              <button
                className={`btn ${
                  modalAction === "reject" ? "btn-danger" : "btn-primary"
                }`}
                onClick={handleConfirm}
                disabled={!!loadingId}
              >
                {loadingId === selectedReg.id ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                  ></span>
                ) : null}
                {modalAction === "approve"
                  ? "Duyệt ngay"
                  : modalAction === "reject"
                  ? "Từ chối ngay"
                  : "Lưu kết quả"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
