// src/components/RegistrationTable.jsx
export default function RegistrationTable({ data, onApproveClick, onRejectClick, onGradeClick, loadingId }) {
  return (
    <table className="table table-hover">
      <thead className="table-light">
        <tr>
          <th>Mã đăng ký</th>
          <th>MSSV</th>
          <th>Lớp</th>
          <th>Họ tên</th>
          <th>Ngày đăng ký</th>
          <th>Người xử lý</th>
          <th>Thời gian xử lý</th>
          <th>Trạng thái</th>
          <th>Điểm</th>
          <th>Hành động</th>
        </tr>
      </thead>
      <tbody>
        {data.length === 0 && (
          <tr>
            <td colSpan="10" className="text-center text-muted">
              Chưa có sinh viên đăng ký
            </td>
          </tr>
        )}

        {data.map((r) => {
          const student = r.student;
          const user = student?.user;

          return (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{student?.studentCode}</td>
              <td>{student?.className}</td>
              <td>{user?.fullName}</td>
              <td>{new Date(r.registeredAt).toLocaleString()}</td>
              <td>{r.reviewer?.user?.fullName || "-"}</td>
              <td>{r.reviewedAt ? new Date(r.reviewedAt).toLocaleString() : "-"}</td>
              <td>
                {r.approved === null ? (
                  <span className="badge bg-secondary">CHƯA DUYỆT</span>
                ) : r.approved === true ? (
                  <span className="badge bg-success">ĐÃ DUYỆT</span>
                ) : (
                  <span className="badge bg-danger" title={r.rejectReason || ""}>TỪ CHỐI</span>
                )}
              </td>
              <td>
                {r.score !== null ? (
                   <span className="fw-bold">{r.score}</span>
                ) : "-"}
              </td>
              <td>
                {r.approved === null && (
                  <>
                    <button
                      className="btn btn-success btn-sm me-1"
                      disabled={loadingId === r.id}
                      onClick={() => onApproveClick(r)}
                    >
                      Duyệt
                    </button>

                    <button
                      className="btn btn-danger btn-sm"
                      disabled={loadingId === r.id}
                      onClick={() => onRejectClick(r)}
                    >
                      Từ chối
                    </button>
                  </>
                )}
                {r.approved === true && (
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={loadingId === r.id}
                      onClick={() => onGradeClick && onGradeClick(r)}
                    >
                      {r.score !== null ? "Sửa điểm" : "Chấm điểm"}
                    </button>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
