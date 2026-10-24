// Nhãn tiếng Việt dùng chung cho các trạng thái — khớp enum ở backend (entity/enums/*Status.java)

export const WORKSPACE_STATUS = {
  DRAFT: "Bản nháp",
  OPEN_TOPIC: "Mở đề tài",
  OPEN_REGISTRATION: "Mở đăng ký",
  LOCK_REGISTRATION: "Khóa đăng ký",
  IN_PROGRESS: "Đang thực hiện",
  CLOSED: "Đã kết thúc",
};

export const TOPIC_STATUS = {
  OPEN: "Đang mở đăng ký",
  FULL: "Đã đủ sinh viên",
  CLOSED: "Đã đóng",
  COMPLETED: "Hoàn thành",
};

export const PROGRESS_STATUS = {
  TODO: "Cần làm",
  IN_PROGRESS: "Đang thực hiện",
  DONE: "Hoàn thành",
};

/** Nhãn của một trạng thái; không có trong bảng thì trả nguyên giá trị. */
export const statusLabel = (table, value) => table[value] ?? value ?? "";

/** Danh sách lựa chọn cho thẻ <select>: [{ value, label }]. */
export const statusOptions = (table) => Object.entries(table).map(([value, label]) => ({ value, label }));
