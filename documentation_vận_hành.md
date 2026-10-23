# QUY TRÌNH VẬN HÀNH & HƯỚNG DẪN SỬ DỤNG TASKIFY

Tài liệu mô tả các quy trình nghiệp vụ và thao tác của từng vai trò trên Taskify — hệ thống quản lý đề tài và tiến độ đồ án. Phần cài đặt/triển khai xem [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

**Vai trò:** `ADMIN` (quản trị hệ thống), `DEPARTMENT_ADMIN` (quản trị khoa), `LECTURER` (giảng viên), `STUDENT` (sinh viên). Quyền chi tiết: [`docs/SECURITY.md`](docs/SECURITY.md#ma-trận-phân-quyền).

---

## 1. Tài khoản, đăng nhập và phiên làm việc

Không có đăng ký công khai: mọi tài khoản do **Admin** tạo (mục *Quản trị hệ thống → Khởi tạo tài khoản*).

**Tạo tài khoản** — Admin chọn một trong hai cách:

| Cách | Người dùng làm gì |
|---|---|
| **Để trống mật khẩu** (cần email hợp lệ) | Nhận **mã kích hoạt** qua email → trang đăng nhập → *Kích hoạt tài khoản* → nhập tên đăng nhập, mã và **tự đặt mật khẩu**. Admin có thể *Gửi lại mã kích hoạt* (cách nhau tối thiểu 60 giây). |
| **Đặt mật khẩu tạm** | Đăng nhập bằng mật khẩu tạm → hệ thống **bắt buộc đổi mật khẩu** trước khi vào trang chủ. |

**Chính sách mật khẩu:** tối thiểu 8 ký tự, có ít nhất một chữ cái và một chữ số, không chứa tên đăng nhập. Ô nhập hiển thị danh sách điều kiện và đánh dấu điều kiện đã đạt.

**Đăng nhập:**
1. Nhập tên đăng nhập + mật khẩu.
2. `ADMIN`, `DEPARTMENT_ADMIN`, `LECTURER`: nhập thêm **mã OTP** gửi qua email (mã dùng một lần; sai 5 lần phải xin mã mới; có nút *Gửi lại mã*).
3. Nhập sai mật khẩu nhiều lần liên tiếp: tài khoản bị tạm khóa trên thiết bị đó trong ít phút (thông báo cho biết thời gian chờ).

**Phiên làm việc:** đăng nhập giữ được **7 ngày** trên trình duyệt (tải lại trang không phải đăng nhập lại). Hệ thống tự làm mới phiên ở nền; đăng xuất ở một tab thì các tab khác cũng đăng xuất. Đổi mật khẩu sẽ đăng xuất mọi thiết bị khác, giữ thiết bị đang dùng. Admin khóa tài khoản hoặc đổi vai trò thì phiên của người đó hết hiệu lực ngay.

**Quên mật khẩu:** trang đăng nhập → *Quên mật khẩu* → nhập **tên đăng nhập hoặc email** → nhận mã qua email → đặt mật khẩu mới.

**Nhập hàng loạt từ Excel** (Admin: *Quản trị hệ thống → Nhập từ Excel*; quản trị khoa: *Quản trị khoa → Nhập từ Excel*, chỉ cho khoa của mình):
1. Chọn **Sinh viên** hoặc **Giảng viên** (Admin chọn thêm khoa) → *Tải file mẫu*.
2. Điền danh sách vào sheet "Danh sách" — sinh viên: MSSV (là tên đăng nhập), họ tên, email, mã lớp, số điện thoại; giảng viên: mã giảng viên, họ tên, email, học vị, chuyên ngành, số điện thoại. Tối đa 2000 dòng/file, tiêu đề cột không cần đúng thứ tự.
3. Chọn file → *Kiểm tra file*: hệ thống báo lỗi **từng dòng** (thiếu dữ liệu, email sai định dạng, trùng trong file, đã có tài khoản, lớp không thuộc khoa…) mà chưa ghi gì. Có thể *Tải danh sách lỗi (CSV)* để sửa.
4. *Nhập N tài khoản hợp lệ*: chỉ các dòng hợp lệ được tạo, dòng lỗi bị bỏ qua. Tài khoản ở trạng thái **chưa kích hoạt**; mỗi người nhận mã kích hoạt qua email (gửi dần để không bị máy chủ email chặn).
5. Mã lớp chưa có trong khoa: báo lỗi, hoặc chọn *Tự tạo lớp chưa có trong khoa*.

**Hồ sơ cá nhân** (*Hồ sơ của tôi*): sửa họ tên, email; đổi mật khẩu (cần mật khẩu hiện tại).

---

## 2. Quản lý Khoa, vai trò và người dùng (Admin)

- **Khoa** (*Quản lý Khoa*): mã khoa (duy nhất, VD `CNTT`), tên khoa, trạng thái hoạt động.
- **Người dùng** (*Danh bạ thành viên*): lọc theo khoa, vai trò, tìm theo tên/tên đăng nhập; phân trang 20 người/trang. Thao tác: khóa/mở khóa, đổi vai trò, gửi lại mã kích hoạt.
- Tài khoản không phải `ADMIN` phải thuộc một khoa. Sinh viên cần **mã sinh viên** và **lớp**; giảng viên có thể nhập học vị, chuyên ngành.
- **Vai trò & quyền** (*Vai trò & quyền*): bật/tắt quyền chi tiết của từng vai trò (VD `TOPIC_MANAGE`). Không xóa được vai trò đang có người dùng.
- **Nhật ký hệ thống**: các thao tác quan trọng (tạo/khóa tài khoản, đổi vai trò, chấm điểm, duyệt chấm lại…) được ghi lại kèm người thực hiện và thời điểm.

---

## 3. Đợt đồ án (Workspace) — Quản trị khoa

Mỗi học kỳ khoa mở một **đợt đồ án** (*Quản trị khoa → Không gian (Workspace)*): tên, loại, học kỳ, thời gian bắt đầu – kết thúc. Đợt đi qua các giai đoạn theo thứ tự:

| Giai đoạn | Việc diễn ra | Ai làm |
|---|---|---|
| **Bản nháp** | Gán **lớp** tham gia (*Phân bổ Lớp*), phân công **giảng viên + quota** số nhóm tối đa (*Giảng viên & Quota*) | Quản trị khoa |
| **Mở đề tài** | Giảng viên được phân công tạo/sửa/xóa đề tài; vẫn gán lớp/phân công được | Giảng viên |
| **Mở đăng ký** | Sinh viên của các lớp đã gán đăng ký đề tài; giảng viên duyệt | Sinh viên, giảng viên |
| **Khóa đăng ký** | Không nhận đăng ký mới; giảng viên duyệt nốt | Giảng viên |
| **Đang thực hiện** | Sinh viên nộp báo cáo tiến độ; giảng viên nhận xét và **chấm điểm** | Sinh viên, giảng viên |
| **Đã kết thúc** | **Điểm bị khóa**; sửa điểm chỉ qua quy trình chấm lại (mục 6) | — |

Nút chuyển giai đoạn chỉ cho phép bước kế tiếp (hoặc *Đóng sớm*). Thao tác sai giai đoạn sẽ bị từ chối kèm thông báo giải thích.

Khác: *Danh mục Lớp* (tạo lớp của khoa), *Duyệt Đề tài* (xem/đóng/mở đề tài trong khoa), *Báo cáo số liệu* (số đợt, nhân sự, sinh viên, đề tài, tỉ lệ lấp đầy chỗ, số giảng viên chưa được phân công).

---

## 4. Giảng viên

- **Đề tài của tôi**: tạo đề tài (tên, mô tả, **số sinh viên tối đa** – `capacity`) trong đợt đang *Mở đề tài*. Hệ thống cảnh báo khi tên trùng/gần trùng đề tài đã có (so sánh không phân biệt dấu tiếng Việt). Không được giảm số chỗ xuống dưới số sinh viên đã duyệt.
- **Duyệt đăng ký** (*Quản lý đăng ký*): xem sinh viên đăng ký từng đề tài → **Duyệt** hoặc **Từ chối** (nhập lý do). Đề tài tự chuyển *Đã đủ sinh viên* khi số sinh viên được duyệt bằng số chỗ.
- **Mốc tiến độ (Milestone)**: đặt các mốc kèm hạn trong thời gian của đợt. Sinh viên được nhắc trước hạn.
- **Theo dõi tiến độ**: xem báo cáo của sinh viên theo đề tài (tải thêm báo cáo cũ), tải file đính kèm, viết nhận xét.
- **Chấm điểm**: thang 0–10 cho **từng sinh viên** khi đợt *Đang thực hiện*. Đề tài chuyển *Hoàn thành* khi mọi sinh viên đã duyệt đều có điểm. Xem lịch sử điểm của từng sinh viên.
- **Xuất Excel**: danh sách đăng ký và điểm (toàn bộ đề tài mình hướng dẫn hoặc một đề tài).
- **Không gian làm việc / Trao đổi học thuật**: thảo luận theo đề tài, nhắn tin riêng với sinh viên của đề tài, thông báo cho đề tài.

---

## 5. Sinh viên

- **Danh sách đề tài**: xem đề tài của các đợt mà lớp mình tham gia; lọc theo trạng thái, tìm theo tên/giảng viên; xem số chỗ còn lại.
- **Đăng ký**: chỉ khi đợt *Mở đăng ký* và đề tài còn chỗ. Mỗi đợt chỉ có **một** đăng ký đang chờ hoặc đã duyệt; muốn đổi đề tài thì hủy đăng ký đang chờ trước (*Đăng ký của tôi*).
- **Theo dõi tiến độ**: khi đợt *Đang thực hiện* và đăng ký đã được duyệt — tạo báo cáo (tiêu đề, nội dung, file đính kèm tối đa 20 MB: pdf, Word, Excel, PowerPoint, zip, rar, ảnh png/jpg, txt), kéo thả trên bảng **Kanban** (Cần làm / Đang thực hiện / Hoàn thành) hoặc xem dạng danh sách. Nhận xét của giảng viên hiện ngay trên thẻ.
- **Kết quả**: xem trạng thái đăng ký, điểm và ngày cập nhật điểm.
- **Trao đổi**: nhắn tin với giảng viên hướng dẫn, tham gia thảo luận của đề tài.

---

## 6. Khóa điểm & quy trình chấm lại

Khi đợt **Đã kết thúc**, điểm bị khóa để bảo đảm kết quả đã công bố không bị sửa tùy ý. Khi cần sửa (nhập nhầm, phúc khảo…):

1. **Giảng viên** mở đăng ký cần sửa → *Yêu cầu chấm lại* → nhập lý do (và thời gian mở khóa mong muốn, mặc định 72 giờ, tối đa 7 ngày).
2. **Quản trị khoa** (hoặc Admin) vào *Yêu cầu chấm lại* → **Duyệt** hoặc **Từ chối**. Không ai tự duyệt yêu cầu của chính mình.
3. Được duyệt: giảng viên sửa điểm **một lần** trong thời gian mở khóa; sửa xong điểm tự khóa lại. Quá hạn mà chưa sửa: yêu cầu hết hiệu lực.
4. Mọi lần chấm/sửa điểm được lưu vào **lịch sử điểm** (điểm cũ → điểm mới, người sửa, thời điểm, yêu cầu chấm lại liên quan).

---

## 7. Thông báo, nhắc lịch và báo cáo

- **Thông báo realtime** (chuông góc trên): đăng ký mới, kết quả duyệt, báo cáo/nhận xét mới, tin nhắn, kết quả yêu cầu chấm lại. Trang *Thông báo* xem toàn bộ (tải thêm theo trang), đánh dấu đã đọc. Một số sự kiện (kết quả duyệt đăng ký, báo cáo tiến độ mới, nhận xét, chấm/sửa điểm, yêu cầu chấm lại, nhắc hạn) được gửi thêm qua email.
- **Nhắc tự động** (giờ Việt Nam): 08:00 hằng ngày nhắc sinh viên có **mốc đến hạn trong 2 ngày**; 09:00 hằng ngày nhắc sinh viên **chưa nộp báo cáo tiến độ trong 7 ngày** (chỉ trong đợt đang thực hiện).
- **Thông báo chung**: Admin gửi toàn hệ thống; giảng viên/khoa gửi theo đề tài.
- **Tổng quan (Dashboard)**: số liệu theo vai trò — Admin toàn hệ thống, khoa theo khoa, giảng viên theo đề tài của mình.
- **Học phí**: **Tạm hoãn — sẽ phát triển sau phiên bản 1.0** (xem [ADR-0005](docs/adr/0005-hoan-tinh-nang-hoc-phi.md)).

---

## 8. Xử lý lỗi thường gặp

Thông báo lỗi luôn bằng tiếng Việt. Khi báo lỗi cho quản trị viên, gửi kèm **mã yêu cầu** (`requestId`) nếu thông báo có hiển thị.

| Hiện tượng | Nguyên nhân thường gặp / cách xử lý |
|---|---|
| Không thấy đề tài nào để đăng ký | Lớp của sinh viên chưa được gán vào đợt, hoặc đợt chưa ở giai đoạn *Mở đăng ký* |
| "Đề tài đã đủ số sinh viên" | Đề tài hết chỗ — chọn đề tài khác hoặc liên hệ giảng viên tăng số chỗ |
| "Bạn đang có một đăng ký chờ duyệt trong đợt này" / "Bạn đã có đề tài được duyệt trong đợt này" | Mỗi đợt chỉ một đăng ký: hủy đăng ký đang chờ ở *Đăng ký của tôi* trước khi đăng ký đề tài khác |
| Thông báo về giai đoạn của đợt (VD "Đợt không ở giai đoạn thực hiện") | Thao tác không thuộc giai đoạn hiện tại của đợt (bảng mục 3) — liên hệ quản trị khoa |
| "Điểm đã bị khóa vì đợt đã kết thúc…" | Gửi yêu cầu chấm lại (mục 6) |
| "Bạn không có quyền thực hiện thao tác này" | Vai trò hoặc phạm vi không cho phép (VD dữ liệu khoa khác); Admin kiểm tra vai trò/quyền |
| Không nhận được OTP / mã kích hoạt | Kiểm tra thư rác; dùng *Gửi lại mã* sau 60 giây; quản trị viên kiểm tra cấu hình email |
| Bị đăng xuất, thông báo "Phiên đăng nhập đã hết hạn" | Phiên quá 7 ngày, đã đổi mật khẩu ở thiết bị khác, hoặc bị khóa tài khoản — đăng nhập lại |
| "Bạn thao tác quá nhanh. Vui lòng thử lại sau N giây." | Vượt giới hạn tần suất (đăng nhập, OTP, tải file) — chờ hết thời gian ghi trong thông báo |

Danh sách mã lỗi đầy đủ cho kỹ thuật viên: [`docs/error-codes.md`](docs/error-codes.md).
