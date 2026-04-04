# QUY TRÌNH VẬN HÀNH & HƯỚNG DẪN SỬ DỤNG HỆ THỐNG QUẢN LÝ ĐÀO TẠO

Tài liệu này mô tả chi tiết các quy trình nghiệp vụ và hướng dẫn người dùng thực hiện các thao tác trên hệ thống Quản lý Đào tạo (Trọng tâm: Quản lý Đề tài & Đồ án tốt nghiệp).

---

## 1. Quy trình Đăng ký & Xác thực Tài khoản
Hệ thống sử dụng cơ chế cấp phát tài khoản tập trung để đảm bảo tính bảo mật và phân quyền chính xác.

*   **Bước 1: Khởi tạo tài khoản**: Quản trị viên (Admin) tạo tài khoản cho Giảng viên/Sinh viên thông qua email và mã định danh.
*   **Bước 2: Xác thực email**: Sau khi tạo, một mã **OTP (One-Time Password)** sẽ được gửi về email đăng ký.
*   **Bước 3: Thiết lập mật khẩu**: Người dùng sử dụng OTP để kích hoạt tài khoản và đặt mật khẩu lần đầu.
*   **Bước 4: Phân quyền**: Dựa trên vai trò (`ADMIN`, `DEPT_ADMIN`, `LECTURER`, `STUDENT`) và các quyền hạn chi tiết (`Privileges`), hệ thống sẽ mở khóa các menu chức năng tương ứng.

---

## 2. Quản lý Khoa & Đơn vị đào tạo
Dành cho Quản trị viên hệ thống.

*   **Thao tác**: Vào mục **Quản trị hệ thống** > **Quản lý Khoa**.
*   **Thông tin bắt buộc**:
    *   **Mã Khoa**: Định danh duy nhất (VD: `CNTT`, `DTVT`).
    *   **Tên Khoa**: Tên đầy đủ của đơn vị.
*   **Thông tin tùy chọn**: Trạng thái hoạt động (Active/Inactive), mô tả khoa.

---

## 3. Quản lý Giảng viên
*   **Quy trình thêm mới**: Admin chọn **Thêm người dùng** > Vai trò: **GIẢNG VIÊN**.
*   **Thông tin cá nhân**: Họ tên, Email, Số điện thoại.
*   **Thông tin chuyên môn**: 
    *   **Học hàm/Học vị**: Tiến sĩ, Thạc sĩ, PGS...
    *   **Chuyên ngành**: Lĩnh vực nghiên cứu chính.
*   **Phân công**: Gán giảng viên vào khoa quản lý để thực hiện phê duyệt đề tài trong khoa đó.

---

## 4. Quản lý Sinh viên & Lớp học
*   **Nhập liệu sinh viên**: Admin hoặc Admin khoa thực hiện.
*   **Dữ liệu học tập**:
    *   **Mã số sinh viên (MSSV)**: Dùng để đăng nhập và định danh.
    *   **Lớp sinh hoạt**: Gán sinh viên vào các lớp cụ thể (VD: `CNTT-K15`).
*   **Phân lớp**: Mỗi lớp được gán vào một **Khoa** để sinh viên có thể đăng ký các đề tài thuộc khoa của mình.

---

## 5. Chức năng dành cho Giảng viên
Giảng viên đóng vai trò là người hướng dẫn và đánh giá đề tài.

*   **Quản lý đề tài**: Tạo các đề tài nghiên cứu, mô tả yêu cầu và "Mở đăng ký" để sinh viên lựa chọn.
*   **Phê duyệt đăng ký**: Xem danh sách sinh viên đăng ký, kiểm tra năng lực và chọn **Phê duyệt** hoặc **Từ chối (kèm lý do)**.
*   **Quản lý tiến độ**: Theo dõi các mốc thời gian (**Milestones**) mà sinh viên đã hoàn thành.
*   **Đánh giá & Chấm điểm**: Nhập điểm tổng kết (Hệ 10) và nhận xét chi tiết sau khi sinh viên hoàn thành báo cáo.
*   **Tài liệu & Trao đổi**: Đăng tải tài liệu hướng dẫn và nhắn tin trực tiếp với sinh viên qua phần **Trao đổi học thuật**.

---

## 6. Chức năng dành cho Sinh viên
Sinh viên là trung tâm của quy trình học tập và nghiên cứu.

*   **Đăng ký đề tài**: Khám phá danh sách đề tài của khoa, xem mô tả và gửi yêu cầu đăng ký cho giảng viên.
*   **Theo dõi tiến độ**: Xem các mốc thời gian thực hiện đồ án, nộp báo cáo tiến độ và đính kèm file minh chứng.
*   **Tra cứu kết quả**: Xem trạng thái đăng ký và điểm số đánh giá từ giảng viên ngay khi có kết quả.
*   **Tương tác**: Nhắn tin với giảng viên để nhận hướng dẫn và phản hồi về lỗi/khó khăn.

---

## 7. Tính năng bổ sung & Báo cáo
*   **Thống kê (Dashboard)**: Biểu đồ trực quan về tỉ lệ hoàn thành đề tài, số lượng đăng ký theo từng khoa.
*   **Xuất báo cáo**: Xuất danh sách sinh viên, điểm số ra file Excel để phục vụ lưu trữ ngoài hệ thống.
*   **Thông báo & Nhắc lịch**: Hệ thống tự động gửi thông báo khi có sinh viên mới đăng ký hoặc khi sắp đến hạn nộp báo cáo (Milestone).
*   **Học phí**: (Tính năng đang phát triển) Theo dõi trạng thái đóng lệ phí thực tập/đồ án.

---

## 8. Bảo mật & Quyền riêng tư
*   **Kiểm soát truy cập (RBAC)**: Đảm bảo người dùng chỉ thấy dữ liệu trong phạm vi được cho phép.
*   **Nhật ký hệ thống (Audit Log)**: Ghi lại mọi hành động thay đổi dữ liệu nhạy cảm để phục vụ kiểm tra khi cần.
*   **Khôi phục mật khẩu**: Người dùng chọn "Quên mật khẩu", nhập email để nhận mã OTP khôi phục quyền truy cập.

---

## 9. Đánh giá & Xử lý lỗi
*   **Tiêu chí đánh giá**: Tỉ lệ sinh viên đăng ký thành công, thời gian phản hồi của giảng viên, tính minh bạch của điểm số.
*   **Xử lý lỗi thường gặp**:
    *   *Không đăng ký được đề tài*: Kiểm tra xem lớp của sinh viên đã được gán vào Workspace (Chu kỳ học) đó chưa.
    *   *Lỗi 403 Forbidden*: Kiểm tra lại quyền hạn (Privileges) đã được admin cấp đúng chưa.

---

## 10. Lộ trình Đào tạo Người dùng
Hệ thống cung cấp tài nguyên đào tạo theo 3 mức độ:

| Cấp độ | Đối tượng | Nội dung đào tạo | Tài liệu |
| :--- | :--- | :--- | :--- |
| **Cơ bản** | Tất cả | Đăng nhập, đổi mật khẩu, xem thông báo, chát. | File PDF hướng dẫn nhanh |
| **Trung cấp** | GV/SV | Quy trình Đăng ký - Duyệt - Nộp báo cáo - Chấm điểm. | Video Demo (5 phút) |
| **Nâng cao** | Admin | Quản lý phân quyền, cấu hình Chu kỳ học (Workspace), Xuất Excel. | Workshop trực tuyến |

---
> [!IMPORTANT]
> Mọi thắc mắc về kỹ thuật hoặc yêu cầu cấp lại quyền truy cập, vui lòng liên hệ bộ phận Quản trị hệ thống qua email: `admin@taskify.edu.vn`.
