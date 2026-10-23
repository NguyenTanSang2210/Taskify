#!/usr/bin/env node
/**
 * Kiểm tra giao diện các tính năng Giai đoạn 4 (Playwright, giống ui-check-gd3.mjs):
 *   (P4-03) nhập sinh viên từ Excel: file mẫu, kiểm tra báo lỗi từng dòng, tải danh sách lỗi, nhập dòng hợp lệ
 *   (P4-01) dashboard theo vai trò: thống kê khoa/đợt (Admin, quản trị khoa), việc cần làm (GV), tiến độ (SV)
 *   (P4-05) mốc tiến độ: giảng viên tạo mốc có trọng số, chấm theo mốc; sinh viên thấy điểm + trạng thái nộp
 *   (P4-06) cài đặt email thông báo trong Hồ sơ: tắt nhóm, chọn tổng hợp mỗi sáng
 *   (P4-02) nhật ký hệ thống: lọc nhanh "Thay đổi điểm", lọc người thực hiện, xem chi tiết, IP, xuất CSV
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

// ---------- P4-01: dashboard theo vai trò ----------
{
  page = await pageAs(dept);
  await page.goto(WEB + "/dashboard");
  await page.getByRole("region", { name: "Thống kê" }).waitFor();
  const overview = await page.getByRole("region", { name: "Thống kê" }).innerText();
  check(/sinh viên đã có đề tài/i.test(overview) && /phân bố điểm/i.test(overview) && /số sinh viên mỗi giảng viên/i.test(overview),
    "P4-01: quản trị khoa thấy thống kê của khoa (tỉ lệ có đề tài, phân bố điểm, tải giảng viên)");
  const apiOverview = (await call("GET", "/dashboard/overview", dept.token)).data;
  check(overview.includes(`${apiOverview.students.withTopic}/${apiOverview.students.total}`), "P4-01: số liệu trên trang khớp API",
    `${apiOverview.students.withTopic}/${apiOverview.students.total} SV có đề tài`);
  const wsOption = apiOverview.workspaces[0];
  if (wsOption) {
    await page.locator("select").filter({ hasText: "Tất cả các đợt" }).selectOption(String(wsOption.id));
    await page.waitForTimeout(1200);
    const filtered = (await call("GET", `/dashboard/overview?workspaceId=${wsOption.id}`, dept.token)).data;
    const t = await page.getByRole("region", { name: "Thống kê" }).innerText();
    check(t.includes(`${filtered.registrations.graded}/${filtered.registrations.approved}`), "P4-01: lọc theo đợt cập nhật số liệu", wsOption.name);
  }
  const [xlsxFile] = await Promise.all([page.waitForEvent("download"), page.getByRole("button", { name: "Xuất Excel" }).click()]);
  check(xlsxFile.suggestedFilename() === "thong-ke.xlsx", "P4-01: xuất thống kê ra Excel", xlsxFile.suggestedFilename());
  await page.screenshot({ path: path.join(SHOTS, "ui-gd4-dashboard-dept.png"), fullPage: true });
  await page.context().close();

  page = await pageAs(admin);
  await page.goto(WEB + "/dashboard");
  await page.getByRole("region", { name: "Thống kê" }).waitFor();
  check(await page.locator("select").filter({ hasText: "Toàn hệ thống" }).isVisible(), "P4-01: Admin chọn được khoa (mặc định toàn hệ thống)");
  await page.context().close();

  const lecturer = await login("lecturer");
  page = await pageAs(lecturer);
  await page.goto(WEB + "/dashboard");
  await page.getByRole("region", { name: "Việc cần làm" }).waitFor();
  const lec = (await call("GET", "/dashboard/lecturer", lecturer.token)).data;
  const lecText = await page.getByRole("region", { name: "Việc cần làm" }).innerText();
  check(/đăng ký chờ duyệt/i.test(lecText) && /chưa nộp tiến độ hơn 7 ngày/i.test(lecText) && lecText.includes(String(lec.pendingRegistrations)),
    "P4-01: giảng viên thấy việc cần làm", `chờ duyệt ${lec.pendingRegistrations}, chưa nhận xét ${lec.uncommentedReports}, SV im lặng ${lec.silentStudents.length}`);
  await page.screenshot({ path: path.join(SHOTS, "ui-gd4-dashboard-lecturer.png"), fullPage: true });
  await page.context().close();

  page = await pageAs(student);
  await page.goto(WEB + "/dashboard");
  await page.getByRole("region", { name: "Đề tài của tôi" }).waitFor();
  const st = (await call("GET", "/dashboard/student", student.token)).data;
  const stText = await page.getByRole("region", { name: "Đề tài của tôi" }).innerText();
  check(st.registration ? stText.includes(st.registration.topicTitle) : /chưa đăng ký đề tài/i.test(stText), "P4-01: sinh viên thấy đề tài + tiến độ thật",
    st.registration ? `${st.registration.status}, tiến độ ${st.progress.percent ?? "—"}%` : "chưa có đề tài");
  check(!(await page.getByText("Gợi ý từ AI").count()), "P4-01: bỏ khối giả lập \"Gợi ý từ AI\" / \"Tiến độ 0%\"");
  await page.screenshot({ path: path.join(SHOTS, "ui-gd4-dashboard-student.png"), fullPage: true });
  await page.context().close();
}

// ---------- P4-05: mốc tiến độ + chấm theo mốc ----------
{
  const lecturer = await login("lecturer");
  const silent = (await call("GET", "/dashboard/lecturer", lecturer.token)).data.silentStudents[0]; // đề tài đang thực hiện có SV được duyệt
  if (!silent) {
    console.log("ℹ️  Bỏ qua P4-05: chưa có đề tài đang thực hiện có sinh viên (chạy scripts/smoke-test.mjs trước).");
  } else {
    const title = "Mốc UI " + Date.now().toString(36);
    page = await pageAs(lecturer);
    await page.goto(WEB + "/project-space");
    await page.locator("select").first().selectOption(String(silent.topicId));
    const panel = page.getByRole("region", { name: "Mốc và chấm điểm" });
    await panel.waitFor();
    await panel.getByRole("button", { name: "Thêm mốc" }).click();
    await panel.locator("#ms-title").fill(title);
    const deadline = new Date(Date.now() + 5 * 864e5);
    const local = new Date(deadline.getTime() - deadline.getTimezoneOffset() * 6e4).toISOString().slice(0, 16);
    await panel.locator("#ms-deadline").fill(local);
    await panel.locator("#ms-weight").fill("100");
    await panel.getByRole("button", { name: "Thêm mốc" }).last().click();
    await panel.getByText("Đã thêm mốc.").waitFor();
    const headerText = await panel.locator("thead th", { hasText: title }).innerText();
    check(/100%/.test(headerText), "P4-05: giảng viên thêm mốc có trọng số", headerText.replace(/\n/g, " · "));
    const row = panel.locator("tbody tr", { hasText: silent.studentCode });
    const col = await panel.locator("thead th").evaluateAll((ths, t) => ths.findIndex((th) => th.innerText.includes(t)), title);
    await row.locator("td").nth(col).getByRole("button").click();
    await row.locator("td").nth(col).getByLabel("Điểm").fill("8.5");
    await row.locator("td").nth(col).getByRole("button", { name: "Lưu" }).click();
    await panel.getByText("Đã lưu điểm.").waitFor();
    const suggestion = (await row.locator("td").last().innerText()).trim();
    check(suggestion.startsWith("8.5"), "P4-05: chấm điểm mốc + điểm đề xuất có trọng số", suggestion.replace(/\n/g, " "));
    await page.screenshot({ path: path.join(SHOTS, "ui-gd4-milestones-lecturer.png"), fullPage: true });
    await page.context().close();

    const username = sql(`SELECT u.username FROM students s JOIN users u ON u.id = s.id WHERE s.student_code = '${silent.studentCode}' LIMIT 1`);
    const sv = await login(username);
    const svBoard = (await call("GET", `/milestones/board?topicId=${silent.topicId}`, sv.token)).data;
    const mine = svBoard.students?.[0];
    const msIdx = (svBoard.milestones || []).findIndex((m) => m.title === title);
    check(svBoard.students?.length === 1 && mine.cells[msIdx]?.score === 8.5, "P4-05: sinh viên chỉ thấy dòng của mình, có điểm mốc", `${svBoard.students?.length} dòng`);
    // trang Không gian làm việc của SV hiển thị đề tài được duyệt ĐẦU TIÊN — chỉ kiểm tra giao diện khi đó là đề tài đang thử
    const regs = (await call("GET", `/registration/mine?studentId=${sv.userId}`, sv.token)).data;
    if (String(regs.filter((r) => r.approved === true)[0]?.topic?.id) === String(silent.topicId)) {
      page = await pageAs(sv);
      await page.goto(WEB + "/project-space");
      const item = page.getByRole("region", { name: "Mốc và chấm điểm" }).locator("li", { hasText: title });
      await item.waitFor();
      const text = await item.innerText();
      check(/8\.5/.test(text) && /Chưa nộp/.test(text) && await item.getByRole("link", { name: "Nộp bài" }).isVisible(),
        "P4-05: giao diện sinh viên: mốc, điểm, trạng thái nộp, nút nộp bài", text.replace(/\n/g, " · "));
      await page.screenshot({ path: path.join(SHOTS, "ui-gd4-milestones-student.png") });
      await page.context().close();
    }
    sql(`DELETE g FROM milestone_grades g JOIN milestones m ON m.id = g.milestone_id WHERE m.title = '${title}'`);
    sql(`DELETE FROM notifications WHERE title LIKE '%${title}%'`);
    sql(`DELETE FROM milestones WHERE title = '${title}'`);
  }
}

// ---------- P4-06: cài đặt email thông báo ----------
{
  page = await pageAs(student);
  await page.goto(WEB + "/profile");
  await page.getByRole("button", { name: "Thông báo" }).click();
  await page.getByLabel("Kết quả đăng ký đề tài").waitFor();
  await page.getByLabel("Nhắc hạn tự động").uncheck();
  await page.getByLabel("Tổng hợp mỗi sáng (07:30)").check();
  await page.getByRole("button", { name: "Lưu cài đặt" }).click();
  await page.getByText("Đã lưu cài đặt thông báo.").waitFor();
  const row = sql(`SELECT CONCAT(email_digest + 0, ':', email_registration + 0, ':', email_reminder + 0) FROM notification_settings WHERE user_id = ${student.userId}`);
  check(row === "1:1:0", "P4-06: lưu cài đặt — tổng hợp mỗi sáng, tắt nhóm nhắc hạn", row);
  await page.reload();
  await page.getByRole("button", { name: "Thông báo" }).click();
  await page.getByLabel("Nhắc hạn tự động").waitFor();
  check(!(await page.getByLabel("Nhắc hạn tự động").isChecked()) && (await page.getByLabel("Tổng hợp mỗi sáng (07:30)").isChecked()),
    "P4-06: tải lại trang vẫn giữ cài đặt");
  await page.screenshot({ path: path.join(SHOTS, "ui-gd4-notification-settings.png") });
  await page.context().close();
  sql(`DELETE FROM notification_settings WHERE user_id = ${student.userId}`);
  sql(`DELETE FROM email_digest_items WHERE user_id = ${student.userId}`);
}

// ---------- P4-02: nhật ký hệ thống ----------
{
  page = await pageAs(admin);
  await page.goto(WEB + "/admin/audit");
  await page.getByRole("link", { name: "Nhật ký hệ thống" }).waitFor();
  await page.locator("tbody tr").first().waitFor();
  const rows = await page.locator("tbody tr").count();
  check(rows > 0 && rows <= 50, "P4-02: trang nhật ký hiển thị tối đa 50 bản ghi mới nhất", `${rows} dòng`);
  const ip = (await page.locator("tbody tr").first().locator("td").nth(4).innerText()).trim();
  check(/^[0-9a-f.:]+$/i.test(ip), "P4-02: ghi IP của người thực hiện", ip);

  await page.getByRole("button", { name: "Thay đổi điểm" }).click();
  await page.waitForTimeout(1200);
  const gradeLabels = await page.locator("tbody tr td:nth-child(3)").allInnerTexts();
  const allGrade = gradeLabels.every((t) => /điểm|chấm lại/i.test(t)) || (await page.getByText("Không có bản ghi phù hợp bộ lọc.").isVisible());
  check(allGrade, "P4-02: lọc nhanh \"Thay đổi điểm\" chỉ còn nhật ký điểm số", `${gradeLabels.length} dòng: ${[...new Set(gradeLabels)].join(", ")}`);

  await page.getByRole("button", { name: "Tất cả" }).click();
  await page.locator("#audit-actor").fill("deptadmin");
  await page.getByRole("button", { name: "Lọc", exact: true }).click();
  await page.waitForTimeout(1200);
  const actors = [...new Set(await page.locator("tbody tr td:nth-child(2)").allInnerTexts())];
  check(actors.length === 1 && actors[0] === "deptadmin", "P4-02: lọc theo người thực hiện", actors.join(", "));
  await page.locator("tbody tr").first().click();
  const detail = await page.locator("dl").first().innerText();
  check(/mã hành động/i.test(detail) && /trình duyệt/i.test(detail), "P4-02: bấm một dòng để xem chi tiết + trình duyệt");
  await page.screenshot({ path: path.join(SHOTS, "ui-gd4-audit.png") });
  const [csv] = await Promise.all([page.waitForEvent("download"), page.getByRole("button", { name: "Xuất CSV" }).click()]);
  const csvText = (await import("node:fs")).readFileSync(await csv.path(), "utf8");
  const csvLines = csvText.trim().split(/\r?\n/);
  check(csv.suggestedFilename() === "nhat-ky-he-thong.csv" && csvLines.length > 1 && csvLines.slice(1).every((l) => l.split(",")[1] === "deptadmin"),
    "P4-02: xuất CSV theo đúng bộ lọc đang chọn", `${csvLines.length - 1} dòng`);
  await page.context().close();
}

await browser.close();
console.log(fails ? `\n${fails} kiểm tra KHÔNG đạt.` : "\nTất cả kiểm tra giao diện GĐ4 đều đạt.");
process.exit(fails ? 1 : 0);
