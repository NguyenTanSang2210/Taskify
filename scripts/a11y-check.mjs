#!/usr/bin/env node
/**
 * P4-11 — kiểm tra accessibility (WCAG 2.1 A/AA) bằng axe-core trên các trang chính của 4 vai trò, chế độ sáng và tối.
 * Chạy trên stack Docker profile dev có dữ liệu demo. Thoát mã 1 nếu còn vi phạm.
 *
 *   npm i --no-save axe-core@4.10.3   (chạy ở thư mục gốc repo; hoặc đặt AXE_PATH tới axe.min.js)
 *   node scripts/a11y-check.mjs                 # cả hai chế độ
 *   SCHEME=dark node scripts/a11y-check.mjs     # chỉ chế độ tối
 *
 * Biến môi trường: API_URL, WEB_URL, DB_CONTAINER (taskify-db), PASSWORD (123456), EDGE_PATH, PW_MODULE, AXE_PATH, SCHEME.
 */
import { execSync } from "node:child_process";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PW_MODULE || "playwright-core");
const AXE = process.env.AXE_PATH || require.resolve("axe-core/axe.min.js");
const API = process.env.API_URL || "http://localhost:8081/api";
const WEB = process.env.WEB_URL || "http://localhost:5175";
const DB = process.env.DB_CONTAINER || "taskify-db";
const PASSWORD = process.env.PASSWORD || "123456";
const EDGE = process.env.EDGE_PATH || "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe";
const sql = (q) => execSync(`docker exec -i ${DB} sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot doan_ltmmt -N -B'`, { input: q }).toString().trim();
async function call(method, path, body) {
  const r = await fetch(API + path, { method, headers: { "Content-Type": "application/json" }, body: body ? JSON.stringify(body) : undefined });
  const d = await r.json();
  d.refreshCookie = r.headers.getSetCookie().map((c) => /^taskify_refresh=([^;]+)/.exec(c)?.[1]).find(Boolean);
  return d;
}
async function login(u) {
  const d = await call("POST", "/auth/login", { username: u, password: PASSWORD });
  if (d.nextStep !== "OTP") return d;
  const code = sql(`SELECT o.code FROM otp_tokens o JOIN users u ON u.id=o.user_id WHERE u.username='${u}' AND o.used=0 AND o.purpose='LOGIN' ORDER BY o.id DESC LIMIT 1`);
  return call("POST", "/auth/otp/verify", { preAuthToken: d.preAuthToken, code });
}
const plan = {
  student: ["/", "/dashboard", "/my-registrations", "/project-space", "/calendar", "/discussion", "/project-chat", "/notifications", "/profile"],
  lecturer: ["/dashboard", "/lecturer-topics", "/lecturer", "/lecturer-progress", "/project-space", "/calendar", "/discussion"],
  admin: ["/dashboard", "/admin", "/admin/roles", "/admin/audit"],
  deptadmin: ["/department-admin"],
};
const browser = await chromium.launch({ executablePath: EDGE });
let total = 0;
for (const scheme of process.env.SCHEME ? [process.env.SCHEME] : ["light", "dark"]) {
  const all = {};
  sql("DELETE FROM login_throttle");
  for (const [user, routes] of Object.entries(plan)) {
    const s = await login(user);
    // bypassCSP: CSP của trang (script-src 'self') chặn script axe được chèn vào
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: scheme, bypassCSP: true });
    await ctx.addCookies([{ name: "taskify_refresh", value: s.refreshCookie, url: WEB + "/api/auth", httpOnly: true, sameSite: "Strict" }]);
    const page = await ctx.newPage();
    for (const r of routes) {
      await page.goto(WEB + r);
      await page.waitForLoadState("networkidle");
      await page.waitForTimeout(800); // chờ hiệu ứng fade-in
      await page.addScriptTag({ path: AXE });
      const res = await page.evaluate(async () => (await window.axe.run(document, { runOnly: { type: "tag", values: ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"] } }))
        .violations.map((v) => ({ id: v.id, impact: v.impact, nodes: v.nodes.map((x) => `${x.target.join(" ")} :: ${(x.any[0]?.message || x.failureSummary || "").slice(0, 120)}`) })));
      for (const v of res) {
        all[v.id] ??= { impact: v.impact, nodes: [] };
        all[v.id].nodes.push(...v.nodes.map((n) => `[${user} ${r}] ${n}`));
      }
    }
    await ctx.close();
  }
  const count = Object.values(all).reduce((n, v) => n + v.nodes.length, 0);
  total += count;
  console.log(`\n${count ? "❌" : "✅"} Chế độ ${scheme === "dark" ? "tối" : "sáng"}: ${count} vi phạm WCAG 2.1 A/AA trên ${Object.values(plan).flat().length} trang`);
  for (const [id, v] of Object.entries(all)) {
    console.log(`  ## ${id} (${v.impact}) — ${v.nodes.length}`);
    v.nodes.slice(0, 5).forEach((n) => console.log("     " + n));
  }
}
await browser.close();
process.exit(total ? 1 : 0);
