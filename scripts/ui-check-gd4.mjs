#!/usr/bin/env node
/**
 * Kiểm tra giao diện các tính năng Giai đoạn 4 (Playwright, giống ui-check-gd3.mjs):
 *   (P4-03) nhập sinh viên từ Excel: file mẫu, kiểm tra báo lỗi từng dòng, tải danh sách lỗi, nhập dòng hợp lệ
 * Chạy trên stack Docker profile dev có dữ liệu demo (DataSeeder).
 *
 * Biến môi trường: API_URL, WEB_URL, DB_CONTAINER (taskify-db), PASSWORD (123456), EDGE_PATH, PW_MODULE, SHOT_DIR.
 */
import { execSync } from "node:child_process";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PW_MODULE || "playwright-core");
import { mkdirSync } from "node:fs";
import path from "node:path";
import { xlsx } from "./lib/xlsx.mjs";
const API = process.env.API_URL || "http://localhost:8081/api";
const WEB = process.env.WEB_URL || "http://localhost:5175";
const DB = process.env.DB_CONTAINER || "taskify-db";
const PASSWORD = process.env.PASSWORD || "123456";
const EDGE = process.env.EDGE_PATH || "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe";
const SHOTS = process.env.SHOT_DIR || path.resolve(process.cwd(), "Plan/reports");
mkdirSync(SHOTS, { recursive: true });
let fails = 0;
const check = (ok, name, detail = "") => { if (!ok) fails++; console.log(`${ok ? "✅ PASS" : "❌ FAIL"}  ${name}${detail ? " — " + detail : ""}`); };
const sql = (q) => execSync(`docker exec -i ${DB} sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql --default-character-set=utf8mb4 -uroot doan_ltmmt -N -B'`, { input: q }).toString().trim();
async function call(method, path, token, body) {
  const r = await fetch(API + path, { method, headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text(); let d; try { d = JSON.parse(t); } catch { d = t; }
  // P3-14: cookie refresh token (HttpOnly) để mở trình duyệt đã đăng nhập sẵn
  const refresh = r.headers.getSetCookie().map((c) => /^taskify_refresh=([^;]+)/.exec(c)?.[1]).find(Boolean);
  if (refresh && d && typeof d === "object") d.refreshCookie = refresh;
  return { status: r.status, data: d };
}
async function login(username) {
  const d = (await call("POST", "/auth/login", null, { username, password: PASSWORD })).data;
  if (d.nextStep !== "OTP") return d;
  const code = sql(`SELECT o.code FROM otp_tokens o JOIN users u ON u.id=o.user_id WHERE u.username='${username}' AND o.used=0 AND o.purpose='LOGIN' ORDER BY o.id DESC LIMIT 1`);
  return (await call("POST", "/auth/otp/verify", null, { preAuthToken: d.preAuthToken, code })).data;
}
const browser = await chromium.launch({ executablePath: EDGE });
// Mỗi trang một phiên mới (refresh token xoay vòng mỗi lần dùng — dùng lại cookie cũ sẽ bị coi là đánh cắp)
async function pageAs(session) {
  const fresh = await login(session.username);
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, timezoneId: "Asia/Ho_Chi_Minh", locale: "vi-VN" });
  await ctx.addCookies([{ name: "taskify_refresh", value: fresh.refreshCookie, url: new URL("/api/auth", WEB).href, httpOnly: true, sameSite: "Strict" }]);
  const page = await ctx.newPage();
  page.on("pageerror", (e) => { fails++; console.log("❌ pageerror", e.message); });
  return page;
}
sql("DELETE FROM login_throttle");
const admin = await login("admin"), student = await login("student"), dept = await login("deptadmin");

let page;

// ---------- P4-03: nhập sinh viên từ Excel ----------
{
  const tag = "gd4imp" + Date.now().toString(36);
  const cls = sql("SELECT c.code FROM academic_classes c JOIN users u ON u.department_id = c.department_id WHERE u.username = 'deptadmin' AND c.active = 1 ORDER BY c.id LIMIT 1");
  const file = xlsx([
    ["MSSV", "Họ tên", "Email", "Mã lớp", "Số điện thoại"],
    [tag + "a", "Nguyễn Văn Import", tag + "a@test.local", cls, "0901234567"],
    [tag + "b", "Trần Thị Import", tag + "b@test.local", cls, ""],
    [tag + "c", "Lê Văn Lỗi", "khong-phai-email", cls, ""],
  ]);
  page = await pageAs(dept);
  await page.goto(WEB + "/department-admin");
  await page.getByRole("button", { name: "Nhập từ Excel" }).click();
  const [template] = await Promise.all([page.waitForEvent("download"), page.getByRole("button", { name: "Tải file mẫu" }).click()]);
  check(template.suggestedFilename() === "mau-nhap-sinh-vien.xlsx", "P4-03: tải file mẫu nhập sinh viên", template.suggestedFilename());

  await page.locator("#import-file").setInputFiles({ name: "danh-sach.xlsx", mimeType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buffer: file });
  await page.getByRole("button", { name: "Kiểm tra file" }).click();
  const summary = page.getByRole("region", { name: "Kết quả kiểm tra" });
  await summary.waitFor();
  const text = await summary.innerText();
  check(/Tổng: 3/.test(text) && /Hợp lệ: 2/.test(text) && /Lỗi: 1/.test(text), "P4-03: kiểm tra file báo đúng số dòng hợp lệ / lỗi", text.split("\n").slice(0, 4).join(" · "));
  check(await page.getByText("Email không hợp lệ").isVisible(), "P4-03: hiện lỗi của từng dòng (chỉ dòng lỗi được lọc sẵn)");
  check(Number(sql(`SELECT COUNT(*) FROM users WHERE username LIKE '${tag}%'`)) === 0, "P4-03: kiểm tra file không ghi vào CSDL");
  const [errors] = await Promise.all([page.waitForEvent("download"), page.getByRole("button", { name: /Tải danh sách lỗi/ }).click()]);
  check(errors.suggestedFilename() === "loi-nhap-sinh-vien.csv", "P4-03: tải danh sách lỗi (CSV)", errors.suggestedFilename());
  await page.screenshot({ path: path.join(SHOTS, "ui-gd4-import-preview.png") });

  page.once("dialog", (d) => d.accept());
  await page.getByRole("button", { name: /Nhập 2 tài khoản hợp lệ/ }).click();
  await page.getByText(/Đã tạo 2 tài khoản/).waitFor({ timeout: 10000 });
  check(true, "P4-03: nhập 2 dòng hợp lệ, bỏ qua dòng lỗi", (await page.getByText(/Đã tạo 2 tài khoản/).textContent()).trim());
  const created = sql(`SELECT CONCAT(u.username, ':', u.activated + 0, ':', r.name, ':', IFNULL(s.student_code, '')) FROM users u JOIN roles r ON r.id = u.role_id LEFT JOIN students s ON s.id = u.id WHERE u.username LIKE '${tag}%' ORDER BY u.username`).split("\n");
  check(created.length === 2 && created.every((c) => /:0:STUDENT:/.test(c)), "P4-03: tài khoản tạo ra là sinh viên CHƯA kích hoạt, có MSSV", created.join(", "));
  const codes = Number(sql(`SELECT COUNT(*) FROM otp_tokens o JOIN users u ON u.id = o.user_id WHERE u.username LIKE '${tag}%' AND o.purpose = 'ACTIVATION'`));
  check(codes === 2, "P4-03: mỗi tài khoản có mã kích hoạt (gửi qua email)", `${codes} mã`);
  await page.context().close();

  // ADMIN phải chọn khoa trước khi kiểm tra
  page = await pageAs(admin);
  await page.goto(WEB + "/admin");
  await page.getByRole("button", { name: "Nhập từ Excel" }).click();
  await page.locator("#import-file").setInputFiles({ name: "danh-sach.xlsx", mimeType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", buffer: file });
  check(await page.getByRole("button", { name: "Kiểm tra file" }).isDisabled() && await page.getByText("Chọn khoa trước khi kiểm tra file.").isVisible(),
    "P4-03: Admin phải chọn khoa trước khi kiểm tra");
  await page.context().close();

  sql(`DELETE o FROM otp_tokens o JOIN users u ON u.id = o.user_id WHERE u.username LIKE '${tag}%'`);
  sql(`DELETE s FROM students s JOIN users u ON u.id = s.id WHERE u.username LIKE '${tag}%'`);
  sql(`DELETE FROM users WHERE username LIKE '${tag}%'`);
}

await browser.close();
console.log(fails ? `\n${fails} kiểm tra KHÔNG đạt.` : "\nTất cả kiểm tra giao diện GĐ4 đều đạt.");
process.exit(fails ? 1 : 0);
