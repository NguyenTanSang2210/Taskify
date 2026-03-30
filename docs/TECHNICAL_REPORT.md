# TECHNICAL REPORT - KTPM

## 1. Tong quan he thong
He thong duoc xay dung de quan ly de tai va cong viec nhom trong moi truong hoc thuat.

Muc tieu:
- Tieu chuan hoa vong doi de tai theo workspace.
- Tang tinh minh bach trong duyet dang ky va cham diem.
- Ho tro cong tac nhom qua luong realtime.

## 2. Pham vi va doi tuong su dung
- ADMIN: Quan tri he thong tong.
- DEPARTMENT_ADMIN: Quan tri khoa (workspace, lop, phan cong GV).
- LECTURER: Quan ly de tai, duyet dang ky, danh gia tien do.
- STUDENT: Dang ky de tai, nop tien do, nhan phan hoi.

## 3. Kien truc ky thuat
### 3.1. Kien truc tang
1. Presentation Layer:
- React pages/components.
2. API Layer:
- Axios client, role-guard frontend.
3. Business Layer:
- Spring REST controllers + services.
4. Data Layer:
- Spring Data JPA repositories + MySQL.
5. Realtime Layer:
- STOMP/SockJS qua `/ws`.

### 3.2. Cong nghe
- Java 21
- Spring Boot 3.4.12
- Spring Security + JWT
- Spring Data JPA + Hibernate
- MySQL
- React 19 + Vite
- Bootstrap

## 4. Phan he nghiep vu chinh
### 4.1. Workspace lifecycle
Trang thai chinh:
- DRAFT
- OPEN_TOPIC
- OPEN_REGISTRATION
- LOCK_REGISTRATION
- IN_PROGRESS
- CLOSED

Y nghia:
- Dieu khien pha nghiep vu: tao de tai, mo dang ky, khoa dang ky, theo doi thuc hien.

### 4.2. Quan ly de tai va dang ky
- Giang vien tao/cap nhat de tai.
- Sinh vien dang ky de tai trong workspace hop le.
- Giang vien duyet/tu choi/cham diem dang ky.

### 4.3. Theo doi tien do
- Tao milestone theo de tai.
- Sinh vien nop bao cao tien do.
- Giang vien cap nhat nhan xet va trang thai cong viec.

### 4.4. Cong tac nhom realtime
- Notification realtime theo user.
- Cap nhat registration/progress realtime theo topic.
- Thao luan thread/post va tin nhan.

## 5. Mo hinh du lieu muc logic
Thuc the chinh:
- User, Role, Department
- Lecturer, Student, AcademicClass
- Workspace, WorkspaceClass, LecturerAssignment
- Topic, TopicRegistration
- Milestone, ProgressReport
- Announcement, Notification
- DiscussionThread, DiscussionPost
- Message

Quan he noi bat:
- Workspace thuoc Department.
- Topic thuoc Workspace va thuoc Lecturer.
- TopicRegistration lien ket Student - Topic.
- ProgressReport thuoc Student + Topic (+ Milestone tuy chon).

## 6. API module map (tom tat)
- `/api/auth`: login, register, OTP.
- `/api/users`: quan tri user va role.
- `/api/workspaces`: quan tri workspace + transition.
- `/api/workspace-classes`: gan/bo gan lop vao workspace.
- `/api/assignments`: phan cong giang vien.
- `/api/topics`: CRUD + open/close de tai.
- `/api/registration`: dang ky, duyet, tu choi, cham diem.
- `/api/progress`: tao bao cao, danh gia tien do.
- `/api/milestones`: quan ly milestone.
- `/api/notifications`: danh sach, dem, read/read-all.
- `/api/discuss`: thread/post thao luan.
- `/api/messages`: inbox/send.
- `/api/export`: xuat excel.

## 7. Bao mat va phan quyen
### 7.1. Xac thuc
- JWT stateless.
- SecurityFilterChain + JWT filter.

### 7.2. Phan quyen
- Theo role va pham vi du lieu (department scope).
- Rule gan voi endpoint va service scope validation.

### 7.3. Quan ly secret
- DB/JWT/mail doc tu environment variables.
- Profile `prod` khong cho fallback seed demo.

## 8. Kiem thu va chat luong
### 8.1. Build/Test
- Backend:
  - `mvnw.cmd -q -DskipTests test-compile`
  - `mvnw.cmd -q test`
- Frontend:
  - `npm run lint`
  - `npm run build`

### 8.2. Test case nghiep vu uu tien
1. Dang nhap theo role + OTP (vai tro yeu cau).
2. Tao workspace va transition dung thu tu.
3. Dang ky de tai theo rang buoc lop/workspace.
4. Nop tien do + nhan xet + nhan thong bao realtime.

## 9. Phi chuc nang
- Bao mat:
  - Tach cau hinh secret.
  - Han che CORS theo origin cau hinh.
- Hieu nang:
  - Co phan trang tren mot so luong nghiep vu.
- Kha nang mo rong:
  - Co the thay broker realtime manh hon neu can (Redis/RabbitMQ/Kafka).

## 10. Gioi han va huong phat trien
Gioi han hien tai:
- Chua co CI/CD va quality gate tu dong.
- Chua co bao cao coverage/chuan benchmark hieu nang.

Huong phat trien:
1. Bo sung CI pipeline (build/lint/test/security scan).
2. Them OpenAPI/Swagger de dong bo API contract.
3. Bo sung observability (structured logging, metrics, tracing).
4. Hoan thien phan dashboard va thong ke nang cao.

## 11. Huong dan su dung tai lieu trong bao cao do an
Goi y gan vao cac chuong:
- Chuong phan tich: muc 1, 2, 4.
- Chuong thiet ke: muc 3, 5, 6.
- Chuong cai dat va kiem thu: muc 7, 8.
- Chuong ket luan: muc 10.
