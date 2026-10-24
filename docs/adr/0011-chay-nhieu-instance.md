# ADR-0011: Chạy nhiều instance backend — RabbitMQ cho realtime, kênh riêng theo id, ShedLock, bộ đếm dùng chung

- **Trạng thái:** Đã chấp nhận
- **Ngày:** 24/10/2026
- **Mã trong kế hoạch:** P4-10 (`Plan/plan1.md`)

## Bối cảnh

Backend giữ một số trạng thái trong bộ nhớ của từng tiến trình: broker STOMP (ai đang theo dõi kênh nào), bộ đếm giới hạn tần suất, và mỗi job định kỳ chạy trên mọi tiến trình. Chạy 2 instance sau nginx sẽ làm: sự kiện phát ở instance A không tới người dùng đang nối WebSocket vào B; giới hạn đăng nhập thành N lần mỗi instance; nhắc hạn / email tổng hợp gửi trùng.

## Quyết định

1. **Realtime**: tùy chọn chuyển tiếp STOMP sang **RabbitMQ** (`app.realtime.relay.enabled`); mặc định vẫn broker trong bộ nhớ cho 1 instance. Tên đích dùng dấu chấm (`/topic/progress.{id}`) vì RabbitMQ không nhận `/` sau tiền tố — cùng một tên chạy được ở cả hai chế độ.
2. **Kênh riêng theo id người dùng** (`/topic/notifications.{userId}`, `/topic/messages.{userId}`), bộ kiểm soát quyền chỉ cho đúng người đó đăng ký — thay cho `/user/queue/*`. Kiểu `/user/...` của Spring cần mỗi instance biết phiên của người dùng ở instance khác; danh sách đó chỉ đồng bộ 10 giây một lần nên thử thật đã mất thông báo khi người dùng vừa tải lại trang. Với kênh theo id, broker phát tới mọi tab trên mọi instance.
3. **ShedLock** (bảng `shedlock`, giờ của CSDL): mỗi job định kỳ chỉ chạy trên một instance mỗi lượt; job dọn bộ đếm trong RAM là ngoại lệ (dữ liệu riêng từng instance).
4. **Giới hạn tần suất dùng chung** qua bảng `rate_limit_windows` (`app.rate-limit.store=jdbc`), không thêm Redis: chỉ các endpoint xác thực dùng bộ đếm (vài chục request/phút), một câu upsert nguyên tử là đủ. Khóa tài khoản sau nhiều lần sai mật khẩu vốn đã lưu ở CSDL.

## Hệ quả

- `docker-compose.scale.yml` chạy RabbitMQ + instance thứ hai cùng tên mạng `backend` (nginx tự chia tải, Prometheus tìm instance qua DNS).
- Mất RabbitMQ: REST vẫn chạy, instance vẫn "ready"; realtime gián đoạn đến khi RabbitMQ trở lại (relay tự nối lại); có health `realtimeBroker`, số liệu `taskify_realtime_broker_up` và cảnh báo.
- File tải lên phải nằm trên ổ dùng chung khi các instance ở nhiều máy (cùng máy: cùng thư mục bind mount).
- Không chọn Redis/Hazelcast: thêm một hệ thống cần vận hành mà lợi ích chưa cần ở quy mô một khoa/trường.
