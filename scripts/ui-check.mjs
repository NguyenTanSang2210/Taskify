#!/usr/bin/env node
/**
 * Kiểm tra giao diện thật bằng Edge headless (Playwright). KHÔNG nằm trong dependency của repo:
 *   cd <thư mục tạm> && npm i playwright-core && node <repo>/scripts/ui-check.mjs
 * (hoặc đặt PW_MODULE trỏ tới playwright-core đã cài.)
 *
 * Kiểm tra: (1) trang đăng nhập không có vi phạm CSP / lỗi console, (2) sinh viên đăng nhập → dashboard,
 * (3) vào các trang tiến độ / thông báo / chat / tài khoản không vi phạm CSP, (4) màn "Kích hoạt tài khoản" mở được
 * và không còn nút "Đăng ký ngay", (5) ADMIN: đăng nhập → bước OTP hiển thị (không vào thẳng hệ thống),
 * (6) 403 chỉ hiện thông báo — vẫn đăng nhập; 401 → về trang đăng nhập kèm lý do, (7) token hết hạn sẵn → tự đăng xuất,
 * (8) tải file đính kèm bằng nút (gửi token).
 * Ảnh chụp lưu ở Plan/reports/ui-*.png.
 *
 * Biến môi trường: WEB_URL (mặc định http://localhost:5175), PASSWORD (123456), EDGE_PATH, PW_MODULE, SHOT_DIR.
 */
import { createRequire } from "node:module";
import { mkdirSync } from "node:fs";
import path from "node:path";

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PW_MODULE || "playwright-core");

const WEB = process.env.WEB_URL || "http://localhost:5175";
const PASSWORD = process.env.PASSWORD || "123456";
const EDGE = process.env.EDGE_PATH || "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe";
const SHOTS = process.env.SHOT_DIR || path.resolve(process.cwd(), "Plan/reports");
mkdirSync(SHOTS, { recursive: true });

let failed = 0;
const report = (ok, name, detail = "") => {
  if (!ok) failed++;
  console.log(`${ok ? "✅ PASS" : "❌ FAIL"}  ${name}${detail ? " — " + detail : ""}`);
};

const browser = await chromium.launch({ executablePath: EDGE, headless: true });

async function freshPage() {
  const ctx = await browser.newContext({ viewport: { width: 1366, height: 800 } });
  const page = await ctx.newPage();
  const problems = [];
  page.on("console", (m) => {
    const t = m.text();
    if (m.type() === "error" && /content security policy|refused to|violat/i.test(t)) problems.push(`CSP: ${t}`);
  });
  page.on("pageerror", (e) => problems.push(`pageerror: ${e.message}`));
  return { ctx, page, problems };
}

try {
  // 1 + 4: trang đăng nhập
  {
    const { ctx, page, problems } = await freshPage();
    await page.goto(`${WEB}/login`, { waitUntil: "networkidle" });
    await page.waitForSelector("input");
    report(problems.length === 0, "Trang đăng nhập: không vi phạm CSP / lỗi script", problems.join(" | "));
    report((await page.getByText("Đăng ký ngay").count()) === 0, "Không còn nút 'Đăng ký ngay'");
    await page.screenshot({ path: path.join(SHOTS, "ui-login.png") });
    await page.getByText("Kích hoạt tài khoản", { exact: true }).first().click();
    await page.waitForSelector("text=Mã kích hoạt");
    report(true, "Màn 'Kích hoạt tài khoản' mở được");
    await page.screenshot({ path: path.join(SHOTS, "ui-activate.png") });
    await ctx.close();
  }

  // 2 + 3: sinh viên
  {
    const { ctx, page, problems } = await freshPage();
    await page.goto(`${WEB}/login`, { waitUntil: "networkidle" });
    await page.fill("input >> nth=0", "student");
    await page.fill("input[type=password]", PASSWORD);
    await page.click("button[type=submit]");
    await page.waitForURL((u) => !u.pathname.startsWith("/login"), { timeout: 15000 });
    report(true, "Sinh viên đăng nhập vào được hệ thống (cổng frontend → backend qua proxy)");
    await page.waitForLoadState("networkidle");
    await page.screenshot({ path: path.join(SHOTS, "ui-student-dashboard.png") });
    for (const route of ["/progress", "/my-registrations", "/notifications", "/project-chat", "/project-space", "/profile"]) {
      await page.goto(`${WEB}${route}`, { waitUntil: "networkidle" });
    }
    report(problems.length === 0, "Các trang sinh viên: không vi phạm CSP / lỗi script", problems.join(" | "));
    await ctx.close();
  }

  // 8 (P2-03): tải file đính kèm bằng nút (gửi kèm token) — thẻ <a href> trước đây bị 401
  {
    const { ctx, page } = await freshPage();
    await page.goto(`${WEB}/login`, { waitUntil: "networkidle" });
    await page.fill("input >> nth=0", "student");
    await page.fill("input[type=password]", PASSWORD);
    await page.click("button[type=submit]");
    await page.waitForURL((u) => !u.pathname.startsWith("/login"), { timeout: 15000 });
    await page.goto(`${WEB}/progress`, { waitUntil: "networkidle" });
    const btn = page.locator("button[title='Tài liệu đính kèm']").first();
    if ((await btn.count()) === 0) {
      report(false, "Tải file đính kèm", "không có báo cáo nào kèm file (chạy smoke-test.mjs trước để tạo dữ liệu)");
    } else {
      const [download] = await Promise.all([page.waitForEvent("download", { timeout: 10000 }), btn.click()]);
      const failure = await download.failure();
      report(!failure && !!download.suggestedFilename(), "Tải file đính kèm bằng nút (có token) thành công",
        failure || `tên file: ${download.suggestedFilename()}`);
    }
    await ctx.close();
  }

  // 6 (P2-02): 403 chỉ báo lỗi, KHÔNG đăng xuất; 401 / token hết hạn → về trang đăng nhập kèm lý do
  {
    const { ctx, page } = await freshPage();
    await page.goto(`${WEB}/login`, { waitUntil: "networkidle" });
    await page.fill("input >> nth=0", "student");
    await page.fill("input[type=password]", PASSWORD);
    await page.click("button[type=submit]");
    await page.waitForURL((u) => !u.pathname.startsWith("/login"), { timeout: 15000 });

    // Giả lập backend từ chối quyền (403) cho một API mà trang thông báo luôn gọi
    await page.route("**/api/notifications**", (r) =>
      r.fulfill({ status: 403, contentType: "application/json", body: JSON.stringify({ status: 403, message: "Out of scope" }) }));
    await page.goto(`${WEB}/notifications`, { waitUntil: "networkidle" });
    const toast = page.getByRole("alert").filter({ hasText: "Bạn không có quyền thực hiện thao tác này" });
    await toast.first().waitFor({ timeout: 5000 }).catch(() => {});
    report((await toast.count()) > 0, "403: hiện thông báo 'Bạn không có quyền…'");
    report(!page.url().includes("/login") && !!(await page.evaluate(() => localStorage.getItem("token"))),
      "403: vẫn giữ đăng nhập (không bị đá ra màn hình đăng nhập)");
    await page.screenshot({ path: path.join(SHOTS, "ui-403-toast.png") });

    // 401 (token bị thu hồi) → đăng xuất, về /login?reason=expired và báo lý do
    await page.unroute("**/api/notifications**");
    await page.route("**/api/notifications**", (r) =>
      r.fulfill({ status: 401, contentType: "application/json", body: JSON.stringify({ status: 401, message: "Unauthorized" }) }));
    await page.goto(`${WEB}/notifications`);
    await page.waitForURL((u) => u.pathname.startsWith("/login"), { timeout: 10000 }).catch(() => {});
    await page.waitForSelector("text=Phiên đăng nhập đã hết hạn", { timeout: 5000 }).catch(() => {});
    report(page.url().includes("reason=expired") && (await page.getByText("Phiên đăng nhập đã hết hạn").count()) > 0,
      "401: về trang đăng nhập và báo 'Phiên đăng nhập đã hết hạn'");
    await ctx.close();
  }

  // 7 (P2-02): token đã hết hạn sẵn trong trình duyệt → tự đăng xuất ngay, không đợi request lỗi
  {
    const { ctx, page } = await freshPage();
    await page.goto(`${WEB}/login`, { waitUntil: "networkidle" });
    await page.evaluate(() => {
      const b64 = (o) => btoa(JSON.stringify(o)).replace(/=+$/, "").replace(/\+/g, "-").replace(/\//g, "_");
      const expired = `${b64({ alg: "HS256" })}.${b64({ sub: "student", exp: Math.floor(Date.now() / 1000) - 60 })}.x`;
      localStorage.setItem("token", expired);
      localStorage.setItem("user", JSON.stringify({ id: 1, username: "student", role: "STUDENT" }));
    });
    await page.goto(`${WEB}/`);
    await page.waitForSelector("text=Phiên đăng nhập đã hết hạn", { timeout: 10000 }).catch(() => {});
    report(page.url().includes("/login") && !(await page.evaluate(() => localStorage.getItem("token"))),
      "Token hết hạn: tự đăng xuất và về trang đăng nhập");
    await ctx.close();
  }

  // 5: ADMIN phải qua bước OTP
  {
    const { ctx, page, problems } = await freshPage();
    await page.goto(`${WEB}/login`, { waitUntil: "networkidle" });
    await page.fill("input >> nth=0", "admin");
    await page.fill("input[type=password]", PASSWORD);
    await page.click("button[type=submit]");
    await page.waitForSelector("text=Xác thực OTP", { timeout: 15000 });
    report(page.url().includes("/login"), "ADMIN đăng nhập → dừng ở bước OTP (chưa vào hệ thống)");
    const stored = await page.evaluate(() => ({ token: localStorage.getItem("token"), temp: localStorage.getItem("temp_token") }));
    report(!stored.token && !stored.temp, "Chưa có token nào trong localStorage trước khi nhập OTP");
    await page.screenshot({ path: path.join(SHOTS, "ui-admin-otp.png") });
    report(problems.length === 0, "Bước OTP: không vi phạm CSP", problems.join(" | "));
    await ctx.close();
  }
} catch (e) {
  report(false, "Lỗi khi chạy kịch bản giao diện", e.message);
} finally {
  await browser.close();
}
console.log(failed === 0 ? "\nTất cả kiểm tra giao diện đều đạt." : `\n${failed} kiểm tra thất bại.`);
process.exit(failed === 0 ? 0 : 1);
