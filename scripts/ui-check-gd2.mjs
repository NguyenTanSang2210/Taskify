#!/usr/bin/env node
/**
 * Kiểm tra giao diện các màn mới của Giai đoạn 2 (Playwright, giống ui-check.mjs):
 *   (P2-09) giảng viên tạo đề tài: chọn đợt + số SV, chặn số SV ngoài 1–10
 *   (P2-10) Admin mở trang Vai trò & quyền hạn, vai trò hệ thống không có nút xóa
 *   (P2-13) đợt đóng → điểm khóa → GV gửi yêu cầu chấm lại → quản trị khoa duyệt → GV sửa một lần
 *           (bắt buộc lý do, bước 0,25) → điểm khóa lại, lịch sử điểm hiển thị đúng
 * Cần chạy scripts/smoke-test.mjs TRƯỚC (để có một sinh viên đã được chấm điểm trong đợt đang thực hiện).
 * Đọc mã OTP từ container DB (dev chưa gửi email thật).
 *
 * Biến môi trường: API_URL, WEB_URL, DB_CONTAINER (ktpm-db), PASSWORD (123456), EDGE_PATH, PW_MODULE, SHOT_DIR.
 */
import { execSync } from "node:child_process";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PW_MODULE || "playwright-core");
import { mkdirSync } from "node:fs";
import path from "node:path";
const API = process.env.API_URL || "http://localhost:8081/api";
const WEB = process.env.WEB_URL || "http://localhost:5175";
const DB = process.env.DB_CONTAINER || "ktpm-db";
const PASSWORD = process.env.PASSWORD || "123456";
const EDGE = process.env.EDGE_PATH || "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe";
const SHOTS = process.env.SHOT_DIR || path.resolve(process.cwd(), "Plan/reports");
mkdirSync(SHOTS, { recursive: true });
let fails = 0;
const check = (ok, name, detail = "") => { if (!ok) fails++; console.log(`${ok ? "✅ PASS" : "❌ FAIL"}  ${name}${detail ? " — " + detail : ""}`); };
const sql = (q) => execSync(`docker exec -i ${DB} sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql --default-character-set=utf8mb4 -uroot doan_ltmmt -N -B'`, { input: q }).toString().trim();
async function call(method, path, token, body) {
  const r = await fetch(API + path, { method, headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text(); let d; try { d = JSON.parse(t); } catch { d = t; } return { status: r.status, data: d };
}
async function login(username) {
  const d = (await call("POST", "/auth/login", null, { username, password: PASSWORD })).data;
  if (d.nextStep !== "OTP") return d;
  const code = sql(`SELECT o.code FROM otp_tokens o JOIN users u ON u.id=o.user_id WHERE u.username='${username}' AND o.used=0 AND o.purpose='LOGIN' ORDER BY o.id DESC LIMIT 1`);
  return (await call("POST", "/auth/otp/verify", null, { preAuthToken: d.preAuthToken, code })).data;
}
const browser = await chromium.launch({ executablePath: EDGE });
async function pageAs(session) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await ctx.addInitScript(([t, u]) => { localStorage.setItem("token", t); localStorage.setItem("user", u); },
    [session.token, JSON.stringify({ id: session.userId, username: session.username, fullName: session.fullName, role: session.role })]);
  const page = await ctx.newPage();
  page.on("pageerror", (e) => { fails++; console.log("❌ pageerror", e.message); });
  return page;
}
sql("DELETE FROM login_throttle");
const dept = await login("deptadmin"), lec = await login("lecturer"), admin = await login("admin");

// ---------- P2-09: GV tạo đề tài có chọn đợt + số SV ----------
const wsName = "Đợt UI GĐ2 " + Date.now();
const ws = (await call("POST", "/workspaces", dept.token, { name: wsName })).data;
await call("POST", `/workspaces/${ws.id}/transition?to=OPEN_TOPIC`, dept.token);
let page = await pageAs(lec);
await page.goto(WEB + "/lecturer-topics");
await page.getByRole("button", { name: /đề tài mới|tạo đề tài|đề xuất/i }).first().click();
await page.waitForTimeout(800);
const options = await page.locator("select[required] option").allTextContents();
check(options.some((o) => o.includes(wsName)), "P2-09: form tạo đề tài liệt kê đợt đang mở nhận đề tài", `${options.length - 1} đợt`);
await page.locator("select[required]").selectOption({ label: wsName });
const topicTitle = "Đề tài tạo từ giao diện " + Date.now();
await page.getByPlaceholder(/VD: Ứng dụng AI/).fill(topicTitle);
await page.locator('input[type="number"]').fill("11");
await page.getByRole("button", { name: /lưu|tạo|xác nhận|gửi/i }).last().click();
await page.waitForTimeout(600);
check(await page.getByText(/từ 1 đến 10/).isVisible(), "P2-09: số SV 11 bị chặn ngay ở form");
await page.locator('input[type="number"]').fill("2");
await page.getByRole("button", { name: /lưu|tạo|xác nhận|gửi/i }).last().click();
await page.waitForTimeout(1200);
const created = sql(`SELECT capacity, workspace_id FROM topics WHERE title='${topicTitle}'`);
check(created === `2\t${ws.id}`, "P2-09: GV tạo đề tài qua giao diện thành công (đúng đợt, 2 chỗ)", created || "không có bản ghi");
await page.screenshot({ path: path.join(SHOTS, "ui-gd2-topic-create.png") });
await page.context().close();

// ---------- P2-10: trang vai trò ----------
page = await pageAs(admin);
await page.goto(WEB + "/admin/roles");
await page.waitForTimeout(1200);
check(await page.getByText("DEPARTMENT_ADMIN").first().isVisible(), "P2-10: Admin mở được /admin/roles");
check(await page.getByRole("link", { name: /Vai trò & quyền hạn/ }).isVisible(), "P2-10: có mục 'Vai trò & quyền hạn' trên sidebar");
check((await page.locator('button[title="Xóa vai trò"]').count()) === 0, "P2-10: vai trò hệ thống không có nút xóa");
await page.screenshot({ path: path.join(SHOTS, "ui-gd2-roles.png") });
await page.context().close();

// ---------- P2-13: khóa điểm & chấm lại ----------
const graded = sql("SELECT r.id, r.topic_id, t.workspace_id, r.score FROM topic_registration r JOIN topics t ON t.id=r.topic_id "
  + "JOIN workspaces w ON w.id=t.workspace_id WHERE r.score IS NOT NULL AND w.status='IN_PROGRESS' ORDER BY r.id DESC LIMIT 1");
if (!graded) {
  console.log("❌ Không có sinh viên đã chấm điểm trong đợt IN_PROGRESS — hãy chạy scripts/smoke-test.mjs trước.");
  process.exit(1);
}
const [regId, topicId, wsId, score] = graded.split("\t");
const closed = await call("POST", `/workspaces/${wsId}/transition?to=CLOSED`, dept.token);
check(closed.status === 200, "Đóng đợt có SV đã chấm điểm", `đăng ký #${regId}, điểm ${score}`);
page = await pageAs(lec);
await page.goto(`${WEB}/lecturer?topicId=${topicId}`);
await page.waitForTimeout(1500);
check(await page.getByText("Đã khóa").first().isVisible(), "P2-13: điểm hiện badge 'Đã khóa' khi đợt đóng");
await page.locator("tr", { hasText: "#" + regId }).hover();
check((await page.locator('button[title="Điều chỉnh điểm số"]').count()) === 0, "P2-13: không còn nút sửa điểm khi đã khóa");
await page.locator('button[title="Yêu cầu chấm lại (điểm đã khóa)"]').first().click();
await page.getByRole("button", { name: "Gửi yêu cầu" }).click();
check(await page.getByText("Vui lòng nêu lý do cần chấm lại.").isVisible(), "P2-13: bắt buộc nhập lý do chấm lại");
await page.locator("textarea").fill("Nhập nhầm điểm bảo vệ (kiểm thử giao diện)");
await page.getByRole("button", { name: "Gửi yêu cầu" }).click();
await page.waitForTimeout(1200);
check(await page.getByText("Chờ duyệt chấm lại").isVisible(), "P2-13: GV gửi yêu cầu → hiện 'Chờ duyệt chấm lại'");
await page.context().close();

page = await pageAs(dept);
await page.goto(`${WEB}/department-admin?tab=grade-revisions`);
await page.waitForTimeout(1500);
const row = page.locator("tr", { hasText: "Nhập nhầm điểm bảo vệ (kiểm thử giao diện)" });
check(await row.isVisible(), "P2-13: quản trị khoa thấy yêu cầu trong tab 'Yêu cầu chấm lại' (mở từ link thông báo)");
await page.screenshot({ path: path.join(SHOTS, "ui-gd2-revision-tab.png") });
await row.getByRole("button", { name: "Duyệt" }).click();
await page.getByRole("button", { name: "Duyệt & mở khóa" }).click();
await page.waitForTimeout(1200);
check(await page.getByText("Đã duyệt và mở khóa điểm.").isVisible(), "P2-13: duyệt yêu cầu thành công");
await page.context().close();

page = await pageAs(lec);
await page.goto(`${WEB}/lecturer?topicId=${topicId}`);
await page.waitForTimeout(1500);
check(await page.getByText(/Được sửa đến/).isVisible(), "P2-13: GV thấy 'Được sửa đến …' sau khi được duyệt");
await page.locator("tr", { hasText: "#" + regId }).hover();
await page.locator('button[title="Điều chỉnh điểm số"]').first().click();
await page.locator('input[type="number"]').fill("9.3");
await page.getByRole("button", { name: "Lưu kết quả" }).click();
check(await page.getByText(/lẻ đến 0,25/).first().isVisible(), "P2-13: điểm 9,3 bị chặn (bước 0,25)");
await page.locator('input[type="number"]').fill("9.25");
await page.getByRole("button", { name: "Lưu kết quả" }).click();
check(await page.getByText("Vui lòng nêu lý do chỉnh sửa điểm.").isVisible(), "P2-13: sửa điểm bắt buộc nêu lý do");
await page.getByPlaceholder("Mọi lần sửa điểm đều được lưu vào lịch sử").fill("Sửa lỗi nhập điểm");
await page.getByRole("button", { name: "Lưu kết quả" }).click();
await page.waitForTimeout(1500);
check(await page.getByText("Đã sửa 1 lần").isVisible() && await page.getByText("Đã khóa").first().isVisible(),
  "P2-13: sửa xong → điểm khóa lại, hiện 'Đã sửa 1 lần'");
await page.locator('button[title="Lịch sử điểm"]').first().click();
await page.waitForTimeout(800);
check(await page.getByText(/→/).first().isVisible() && await page.getByText("Sửa lỗi nhập điểm").isVisible(), "P2-13: modal lịch sử điểm hiện điểm cũ → mới và lý do");
await page.screenshot({ path: path.join(SHOTS, "ui-gd2-grade-history.png") });
await page.context().close();
await browser.close();
console.log(fails ? `\n${fails} kiểm tra KHÔNG đạt` : "\nTất cả kiểm tra giao diện GĐ2 đều đạt.");
process.exit(fails ? 1 : 0);
