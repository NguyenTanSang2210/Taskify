#!/usr/bin/env node
/**
 * Kiểm tra giao diện các thay đổi của Giai đoạn 3 (Playwright, giống ui-check-gd2.mjs):
 *   (P3-06) danh bạ thành viên phân trang + tìm kiếm phía máy chủ; thông báo "Tải thêm"; chat tải theo hội thoại
 *   (P3-02/P3-03) API trả lỗi chuẩn {code, message tiếng Việt, errors[]}
 * Nên chạy scripts/smoke-test.mjs TRƯỚC (có tin nhắn giảng viên → sinh viên để kiểm tra chat).
 *
 * Biến môi trường: API_URL, WEB_URL, DB_CONTAINER (taskify-db), PASSWORD (123456), EDGE_PATH, PW_MODULE, SHOT_DIR.
 */
import { execSync } from "node:child_process";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PW_MODULE || "playwright-core");
import { mkdirSync } from "node:fs";
import path from "node:path";
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

// ---------- P3-06: danh bạ thành viên có phân trang + tìm kiếm ----------
let page = await pageAs(admin);
await page.goto(WEB + "/admin");
await page.getByText("Danh bạ thành viên").first().click();
const deptSelect = page.locator("select").filter({ hasText: "Chọn khoa..." });
const cntt = (await deptSelect.locator("option").allTextContents()).find((o) => o.startsWith("CNTT -"));
await deptSelect.selectOption({ label: cntt }); // khoa mẫu (DataSeeder) có giảng viên/sinh viên demo
await page.waitForTimeout(1500);
const pagerText = (await page.locator('nav[aria-label="Phân trang"], p:has-text("mục")').first().textContent()) || "";
check(/mục/.test(pagerText), "P3-06: danh bạ thành viên hiện thanh phân trang", pagerText.trim());
const rowsBefore = await page.locator("tbody tr").count();
check(rowsBefore > 0 && rowsBefore <= 20, "P3-06: tối đa 20 thành viên mỗi trang", `${rowsBefore} dòng`);
if (await page.locator('button[aria-label="Trang sau"]').count()) {
  await page.locator('button[aria-label="Trang sau"]').click();
  await page.waitForTimeout(1000);
  check((await page.locator("nav[aria-label='Phân trang']").textContent()).includes("Trang 2/"), "P3-06: chuyển sang trang 2");
}
await page.getByPlaceholder(/Tên đăng nhập, họ tên, email/).fill("lecturer");
await page.getByPlaceholder(/Tên đăng nhập, họ tên, email/).press("Enter");
await page.waitForTimeout(1200);
const found = await page.locator("tbody tr", { hasText: "lecturer" }).count();
check(found >= 1 && (await page.locator("tbody tr").count()) <= 20, "P3-06: tìm kiếm thành viên phía máy chủ", `${found} kết quả`);
await page.screenshot({ path: path.join(SHOTS, "ui-gd3-users-paged.png") });
await page.context().close();

// ---------- P3-06: thông báo "Tải thêm" ----------
const values = Array.from({ length: 25 }, (_, i) => `(${student.userId}, 'INFO', 'Thông báo phân trang ${i}', NOW(6), 'INFO')`).join(",");
sql(`INSERT INTO notifications (user_id, type, title, created_at, severity) VALUES ${values}`);
page = await pageAs(student);
await page.goto(WEB + "/notifications");
await page.waitForTimeout(1500);
const moreBtn = page.getByRole("button", { name: /Tải thêm/ });
check(await moreBtn.isVisible(), "P3-06: trang thông báo chỉ tải 20 mục đầu, có nút 'Tải thêm'", await moreBtn.textContent());
const beforeMore = await page.getByText(/Thông báo phân trang \d+/).count();
await moreBtn.click();
await page.waitForTimeout(1200);
const afterMore = await page.getByText(/Thông báo phân trang \d+/).count();
check(afterMore > beforeMore, "P3-06: bấm 'Tải thêm' nối thêm thông báo cũ hơn", `${beforeMore} → ${afterMore}`);
await page.context().close();

// ---------- P3-06: chat dùng hội thoại theo đề tài ----------
const last = sql(`SELECT m.content FROM messages m JOIN users u ON u.id = m.recipient_id WHERE u.username = 'student' ORDER BY m.id DESC LIMIT 1`);
if (last) {
  page = await pageAs(student);
  await page.goto(WEB + "/project-chat");
  await page.waitForTimeout(2500);
  check(await page.getByText(last).first().isVisible(), "P3-06: chat hiện tin nhắn mới nhất của hội thoại", last);
  await page.screenshot({ path: path.join(SHOTS, "ui-gd3-chat.png") });
  await page.context().close();
} else {
  console.log("ℹ️  Bỏ qua kiểm tra chat: chưa có tin nhắn (chạy scripts/smoke-test.mjs trước).");
}

// ---------- P3-11: realtime vẫn chạy khi thư viện WebSocket được tải sau (lazy) ----------
{
  sql("UPDATE notifications SET read_at = NOW(6) WHERE user_id = " + student.userId);
  page = await pageAs(student);
  await page.goto(WEB + "/dashboard");
  await page.waitForTimeout(3000); // kết nối WebSocket
  const title = "Thông báo realtime " + Date.now();
  const sent = await call("POST", `/notifications/create?userId=${student.userId}&title=${encodeURIComponent(title)}`, admin.token);
  await page.waitForTimeout(2500);
  const badge = (await page.locator("button:has(span:text('notifications')) span.rounded-full").first().textContent().catch(() => "")) || "";
  check(sent.status === 200 && badge.trim() === "1", "P3-11: chuông nhận thông báo realtime (WebSocket tải lazy)", `huy hiệu: '${badge.trim()}'`);
  await page.context().close();
  sql(`DELETE FROM notifications WHERE title = '${title}'`);
}

// ---------- P3-03/P3-02: lỗi kiểm tra dữ liệu hiển thị bằng tiếng Việt ----------
const bad = await call("POST", "/workspaces", dept.token, { name: "", startAt: "31/12/2030" });
check(bad.status === 400 && bad.data?.code === "VALIDATION_FAILED" && /[À-ỹ]/.test(bad.data?.message || ""),
  "P3-02/P3-03: API trả lỗi chuẩn {code, message tiếng Việt, errors[]}", `${bad.data?.code}: ${bad.data?.message}`);

await browser.close();
sql(`DELETE FROM notifications WHERE title LIKE 'Thông báo phân trang %'`);
console.log(fails ? `\n${fails} kiểm tra KHÔNG đạt.` : "\nTất cả kiểm tra giao diện GĐ3 đều đạt.");
process.exit(fails ? 1 : 0);
