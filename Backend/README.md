# Backend - KTPM

## 1. Tong quan
Backend la dich vu Spring Boot cung cap:
- REST API cho frontend (`/api/*`)
- Xac thuc va phan quyen theo JWT + role
- Kenh realtime qua WebSocket STOMP (`/ws`)
- Xu ly nghiep vu quan ly de tai, workspace, tien do, thong bao

Backend dong vai tro trung tam nghiep vu: tiep nhan request, kiem tra quyen, xu ly logic, luu tru MySQL va phat su kien realtime.

## 2. Cong nghe chinh
- Java 21
- Spring Boot 3.4.12
- Spring Security + JWT
- Spring Data JPA + Hibernate
- MySQL 8
- STOMP/SockJS WebSocket
- Maven Wrapper (`mvnw`, `mvnw.cmd`)

## 3. Cau truc package chinh
- `config`: security, CORS, websocket config.
- `security`: JWT filter/provider, websocket auth interceptor.
- `controller`: khai bao endpoint REST theo domain nghiep vu.
- `service`: logic xu ly, email, otp, audit, security scope.
- `entity`: model du lieu JPA.
- `repository`: truy cap du lieu.
- `dto`: object truyen du lieu auth/login.
- `scheduler`: tac vu nen theo lich.

## 4. Nhom endpoint nghiep vu
- Auth + User + Role
- Workspace, lop hoc thuat, phan cong giang vien
- Topic, topic registration, progress, milestone
- Discussion, message, notification
- Dashboard, report/export, file upload/secure file

## 5. Luong xu ly backend so luoc
1. Nhan request REST tu frontend.
2. JWT filter xac thuc token.
3. Security scope kiem tra quyen role/pham vi khoa/du an.
4. Controller goi service xu ly nghiep vu.
5. Service thao tac repository/entity de doc ghi MySQL.
6. Neu co su kien can push, gui thong bao qua websocket broker.
7. Tra response JSON ve frontend.

## 6. Cau hinh moi truong
Cac file cau hinh:
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`
- `.env.example`

Khuyen nghi:
- Dung bien moi truong cho DB/JWT/MAIL.
- Khong commit file `.env` that.
- Seed demo data chi bat trong profile `dev`.

## 7. Chay backend local
Tu thu muc `Backend/`:

```powershell
# chay backend
.\mvnw.cmd spring-boot:run
```

Kiem tra nhanh:

```powershell
# compile test sources
.\mvnw.cmd -q -DskipTests test-compile

# run test
.\mvnw.cmd -q test
```

## 8. Chay backend bang Docker
Backend duoc dong goi qua `Backend/Dockerfile` va duoc dung trong `../docker-compose.yml`.

Chay full stack tu root project:

```powershell
cd ..
docker compose up --build -d
```

## 9. Tham khao
- Tong quan du an: `../README.md`
- Frontend module: `../frontend/README.md`
- Tai lieu ky thuat: `../docs/TECHNICAL_REPORT.md`
