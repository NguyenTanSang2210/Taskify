#!/usr/bin/env node
/**
 * Taskify — Smoke test toàn luồng nghiệp vụ (Plan P0-05).
 *
 * Chạy khi stack Docker đang chạy với profile dev + seed:
 *   node scripts/smoke-test.mjs
 *
 * Biến môi trường (tùy chọn):
 *   API_URL       mặc định http://localhost:8081/api   (gọi thẳng backend)
 *   WEB_URL       mặc định http://localhost:5175       (đi qua nginx của frontend)
 *   DB_CONTAINER  mặc định ktpm-db                     (để đọc mã OTP từ DB)
 *   REPORT_FILE   nếu đặt: ghi kết quả dạng Markdown ra file này
 *
 * Kết quả mỗi kịch bản:
 *   OK    — hoạt động đúng
 *   BUG   — tái hiện đúng lỗi đã biết trong Plan (ghi kèm mã task sẽ sửa)
 *   FIXED — lỗi đã biết không còn tái hiện (đã được sửa) → cập nhật Plan
 *   FAIL  — lỗi ngoài dự kiến → cần điều tra (script trả exit code 1)
 */
import { execSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const API = process.env.API_URL || "http://localhost:8081/api";
const WEB = process.env.WEB_URL || "http://localhost:5175";
const API_ORIGIN = new URL(API).origin;
const DB_CONTAINER = process.env.DB_CONTAINER || "ktpm-db";
const PASSWORD = "123456";
const RUN = Date.now().toString().slice(-6);

const env = loadDotEnv(join(ROOT, ".env"));
const DB_USER = env.MYSQL_USER || "ktpm";
const DB_PASS = env.MYSQL_PASSWORD || "ktpm123";
const DB_NAME = env.MYSQL_DATABASE || "doan_ltmmt";

const results = [];
const ctx = {};

// ───────────────────────── helpers ─────────────────────────

function loadDotEnv(path) {
  if (!existsSync(path)) return {};
  const out = {};
  for (const line of readFileSync(path, "utf8").split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m) out[m[1]] = m[2];
  }
  return out;
}

async function call(method, path, { token, body, query, form, base = API, timeoutMs = 30000 } = {}) {
  const url = new URL(base + path);
  for (const [k, v] of Object.entries(query || {})) url.searchParams.set(k, String(v));
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const res = await fetch(url, { method, headers, body: payload, signal: AbortSignal.timeout(timeoutMs) });
  const type = res.headers.get("content-type") || "";
  let data;
  if (type.includes("application/json")) data = await res.json().catch(() => null);
  else if (type.includes("text")) data = await res.text();
  else data = Buffer.from(await res.arrayBuffer());
  return { status: res.status, data, type };
}

function sql(query) {
  const cmd = `docker exec -e MYSQL_PWD=${DB_PASS} ${DB_CONTAINER} mysql -u${DB_USER} ${DB_NAME} -N -B -e "${query}"`;
  return execSync(cmd, { stdio: ["ignore", "pipe", "ignore"] }).toString().trim();
}

function latestOtp(username) {
  return sql(
    `SELECT o.code FROM otp_tokens o JOIN users u ON u.id=o.user_id ` +
    `WHERE u.username='${username}' AND o.used=0 ORDER BY o.id DESC LIMIT 1`
  );
}

async function login(username, password = PASSWORD) {
  const r = await call("POST", "/auth/login", { body: { username, password } });
  if (r.status !== 200) throw new Error(`login ${username} → HTTP ${r.status}`);
  const d = r.data;
  if (d.otpRequired) {
    const code = latestOtp(username);
    const v = await call("POST", "/auth/otp/verify", { body: { username, code } });
    if (!v.data?.verified) throw new Error(`OTP ${username} không xác thực được`);
  }
  return { token: d.token, id: d.userId, role: d.role, otpRequired: d.otpRequired };
}

function record(id, name, status, detail, task = "") {
  results.push({ id, name, status, detail, task });
  const icon = { OK: "✅", BUG: "🐞", FIXED: "🎉", FAIL: "❌", SKIP: "⏭️" }[status];
  console.log(`${icon} [${id}] ${name} — ${status}${task ? ` (${task})` : ""}: ${detail}`);
}

/** Kịch bản phải chạy đúng. fn trả về chuỗi mô tả; ném lỗi nếu sai. */
async function step(id, name, fn) {
  try {
    record(id, name, "OK", (await fn()) || "");
    return true;
  } catch (e) {
    record(id, name, "FAIL", e.message);
    return false;
  }
}

/** Kịch bản tái hiện lỗi đã biết. fn trả về { bug: boolean, detail }. */
async function knownBug(id, name, task, fn) {
  try {
    const { bug, detail } = await fn();
    record(id, name, bug ? "BUG" : "FIXED", detail, task);
  } catch (e) {
    record(id, name, "FAIL", e.message, task);
  }
}

function expectStatus(r, expected, what) {
  const list = Array.isArray(expected) ? expected : [expected];
  if (!list.includes(r.status)) {
    const msg = typeof r.data === "object" && r.data ? r.data.message || JSON.stringify(r.data).slice(0, 160) : String(r.data).slice(0, 160);
    throw new Error(`${what}: HTTP ${r.status} (mong đợi ${list.join("/")}) ${msg}`);
  }
  return r.data;
}

// ───────────────────────── kịch bản ─────────────────────────

async function main() {
  console.log(`\nTaskify smoke test — run #${RUN}\nAPI=${API}  WEB=${WEB}  DB=${DB_CONTAINER}\n`);

  const health = await call("POST", "/auth/login", { body: { username: "x", password: "y" } }).catch(() => null);
  if (!health) {
    console.error("❌ Không kết nối được backend. Hãy chạy: docker compose up -d");
    process.exit(2);
  }

  // 1 — Admin + OTP (kèm kiểm tra lỗi P1-01: token dùng được trước khi nhập OTP)
  await knownBug("1a", "Token ADMIN dùng được TRƯỚC khi xác thực OTP", "P1-01", async () => {
    const r = await call("POST", "/auth/login", { body: { username: "admin", password: PASSWORD } });
    const pre = await call("GET", "/users", { token: r.data.token });
    return { bug: pre.status === 200, detail: `GET /users bằng token chưa qua OTP → HTTP ${pre.status}` };
  });
  await step("1", "Admin đăng nhập + OTP", async () => {
    ctx.admin = await login("admin");
    return `role=${ctx.admin.role}, otpRequired=${ctx.admin.otpRequired}`;
  });

  // 2 — Admin tạo khoa + tạo user giảng viên
  await step("2", "Admin tạo khoa và tạo giảng viên mới", async () => {
    const depts = expectStatus(await call("GET", "/departments", { token: ctx.admin.token }), 200, "GET departments");
    ctx.cntt = depts.find((d) => d.code === "CNTT");
    if (!ctx.cntt) throw new Error("Không thấy khoa CNTT (seed chưa chạy?)");
    expectStatus(await call("POST", "/departments", { token: ctx.admin.token, body: { code: `T${RUN}`, name: `Khoa thử ${RUN}` } }), 200, "Tạo khoa");
    const roles = expectStatus(await call("GET", "/users/roles", { token: ctx.admin.token }), 200, "GET roles");
    const lecRole = roles.find((r) => r.name === "LECTURER");
    ctx.lecturer2Name = `lect${RUN}`;
    expectStatus(await call("POST", "/users", {
      token: ctx.admin.token,
      body: { username: ctx.lecturer2Name, password: PASSWORD, fullName: `GV thử ${RUN}`, roleId: lecRole.id, departmentId: ctx.cntt.id },
    }), 200, "Tạo giảng viên");
    return `khoa T${RUN}, giảng viên ${ctx.lecturer2Name}`;
  });

  // 3 — DeptAdmin tạo workspace → OPEN_TOPIC
  await step("3", "DeptAdmin tạo workspace → OPEN_TOPIC", async () => {
    ctx.dept = await login("deptadmin");
    const w = expectStatus(await call("POST", "/workspaces", {
      token: ctx.dept.token, body: { name: `Smoke ${RUN}`, type: "DO_AN", semester: "HK1" },
    }), 200, "Tạo workspace");
    ctx.wsId = w.id;
    const t = expectStatus(await call("POST", `/workspaces/${w.id}/transition`, { token: ctx.dept.token, query: { to: "OPEN_TOPIC" } }), 200, "Transition");
    return `workspace #${w.id} status=${t.status}`;
  });

  // 4 — Gán lớp + phân công GV
  await step("4", "DeptAdmin gán lớp CNTT-K15 + phân công giảng viên", async () => {
    ctx.lecturer = await login("lecturer");
    const classes = expectStatus(await call("GET", "/classes", { token: ctx.dept.token, query: { departmentId: ctx.cntt.id } }), 200, "GET classes");
    const k15 = classes.find((c) => c.code === "CNTT-K15");
    if (!k15) throw new Error("Không thấy lớp CNTT-K15 (seed P0-04?)");
    expectStatus(await call("POST", "/workspace-classes/assign", { token: ctx.dept.token, body: { workspaceId: ctx.wsId, classId: k15.id } }), 200, "Gán lớp");
    expectStatus(await call("POST", "/assignments/assign", { token: ctx.dept.token, body: { workspaceId: ctx.wsId, lecturerId: ctx.lecturer.id, type: "MAIN" } }), 200, "Phân công");
    return `lớp #${k15.id}, GV #${ctx.lecturer.id}`;
  });

  // 5 — Giảng viên tạo đề tài (lỗi P2-01) → dự phòng: DeptAdmin tạo thay để luồng tiếp tục
  const topicBody = { title: `Đề tài smoke ${RUN}`, description: "Kiểm thử tự động", capacity: 3, workspace: { id: ctx.wsId } };
  await knownBug("5", "Giảng viên tự tạo đề tài", "P2-01", async () => {
    const r = await call("POST", "/topics/create", { token: ctx.lecturer.token, query: { lecturerId: ctx.lecturer.id }, body: topicBody });
    if (r.status === 200) ctx.topicId = r.data.id;
    return { bug: r.status === 403, detail: `HTTP ${r.status}${r.status === 403 ? " — thiếu quyền TOPIC_MANAGE (frontend sẽ đăng xuất GV)" : ""}` };
  });
  if (!ctx.topicId) {
    await step("5b", "DeptAdmin tạo đề tài thay giảng viên (đường vòng)", async () => {
      const t = expectStatus(await call("POST", "/topics/create", { token: ctx.dept.token, query: { lecturerId: ctx.lecturer.id }, body: topicBody }), 200, "Tạo đề tài");
      ctx.topicId = t.id;
      return `đề tài #${t.id}, capacity=${t.capacity}`;
    });
  }
  if (!ctx.topicId) return;

  // 6 — Mở đăng ký
  await step("6", "DeptAdmin chuyển workspace → OPEN_REGISTRATION", async () => {
    const t = expectStatus(await call("POST", `/workspaces/${ctx.wsId}/transition`, { token: ctx.dept.token, query: { to: "OPEN_REGISTRATION" } }), 200, "Transition");
    return `status=${t.status}`;
  });

  // 7, 8 — Sinh viên đăng ký
  await step("7", "Student đăng ký đề tài", async () => {
    ctx.student = await login("student");
    const r = expectStatus(await call("POST", "/registration/register", { token: ctx.student.token, query: { studentId: ctx.student.id, topicId: ctx.topicId } }), 200, "Đăng ký");
    return `đăng ký #${r.id}, approved=${r.approved} (chờ duyệt)`;
  });
  await step("8", "Student2 đăng ký cùng đề tài (capacity=3)", async () => {
    ctx.student2 = await login("student2");
    const r = expectStatus(await call("POST", "/registration/register", { token: ctx.student2.token, query: { studentId: ctx.student2.id, topicId: ctx.topicId } }), 200, "Đăng ký");
    return `đăng ký #${r.id}`;
  });

  // 9 — Duyệt 2 sinh viên (lỗi P2-04: chỉ duyệt được 1)
  await step("9a", "Giảng viên duyệt Student", async () => {
    const regs = expectStatus(await call("GET", `/registration/topic/${ctx.topicId}`, { token: ctx.lecturer.token }), 200, "GET registrations");
    ctx.reg1 = regs.find((r) => r.studentId === ctx.student.id)?.id;
    ctx.reg2 = regs.find((r) => r.studentId === ctx.student2.id)?.id;
    const a = expectStatus(await call("POST", `/registration/approve/${ctx.reg1}`, { token: ctx.lecturer.token }), 200, "Duyệt");
    return `đăng ký #${ctx.reg1} approved=${a.approved}`;
  });
  await knownBug("9b", "Giảng viên duyệt Student2 (đề tài capacity=3)", "P2-04", async () => {
    const r = await call("POST", `/registration/approve/${ctx.reg2}`, { token: ctx.lecturer.token });
    return { bug: r.status === 400, detail: `HTTP ${r.status} ${r.data?.message || ""}` };
  });

  // 10 — Khóa đăng ký → thực hiện
  await step("10", "DeptAdmin → LOCK_REGISTRATION → IN_PROGRESS", async () => {
    expectStatus(await call("POST", `/workspaces/${ctx.wsId}/transition`, { token: ctx.dept.token, query: { to: "LOCK_REGISTRATION" } }), 200, "LOCK");
    const t = expectStatus(await call("POST", `/workspaces/${ctx.wsId}/transition`, { token: ctx.dept.token, query: { to: "IN_PROGRESS" } }), 200, "IN_PROGRESS");
    return `status=${t.status}`;
  });

  // 11 — Nộp báo cáo kiểu ProjectSpacePage (fileUrl là object) — lỗi P2-03
  await knownBug("11", "Nộp báo cáo kèm file từ trang Không gian dự án", "P2-03", async () => {
    const r = await call("POST", "/progress/create", {
      token: ctx.student.token, query: { studentId: ctx.student.id, topicId: ctx.topicId },
      body: { title: "Báo cáo tuần 1", content: "Nội dung", fileUrl: { fileName: "a.pdf", fileUrl: "/api/secure/files/a.pdf" } },
    });
    return { bug: r.status === 400, detail: `gửi fileUrl dạng object như ProjectSpacePage.jsx:126 → HTTP ${r.status}` };
  });

  // 12 — Upload + nộp báo cáo đúng định dạng
  await step("12", "Upload file + nộp báo cáo (trang Chi tiết tiến độ)", async () => {
    const form = new FormData();
    form.append("file", new Blob([Buffer.from("Taskify smoke test file")], { type: "text/plain" }), `smoke-${RUN}.txt`);
    const up = expectStatus(await call("POST", "/upload", { token: ctx.student.token, form }), 200, "Upload");
    ctx.fileUrl = up.fileUrl;
    const pr = expectStatus(await call("POST", "/progress/create", {
      token: ctx.student.token, query: { studentId: ctx.student.id, topicId: ctx.topicId },
      body: { title: "Báo cáo tuần 1", content: "Đã hoàn thành phân tích yêu cầu", fileUrl: up.fileUrl },
    }), 200, "Nộp báo cáo");
    ctx.prId = pr.id;
    return `báo cáo #${pr.id}, file ${up.fileUrl}`;
  });

  // 13 — Tải file: <a href> không gửi token (lỗi P2-03) / có token thì được
  await knownBug("13a", "Tải file bằng link <a href> (không có header Authorization)", "P2-03", async () => {
    const r = await call("GET", ctx.fileUrl, { base: API_ORIGIN });
    return { bug: r.status === 401 || r.status === 403, detail: `HTTP ${r.status}` };
  });
  await step("13b", "Giảng viên tải file có kèm token", async () => {
    const data = expectStatus(await call("GET", ctx.fileUrl, { base: API_ORIGIN, token: ctx.lecturer.token }), 200, "Download");
    return `${data.length} bytes`;
  });

  // 14 — Nhận xét + thông báo
  await step("14", "Giảng viên nhận xét → sinh viên nhận thông báo", async () => {
    expectStatus(await call("PUT", `/progress/${ctx.prId}/comment`, { token: ctx.lecturer.token, body: { lecturerComment: "Làm tốt, bổ sung sơ đồ use case" } }), 200, "Nhận xét");
    const ns = expectStatus(await call("GET", "/notifications/mine", { token: ctx.student.token, query: { userId: ctx.student.id } }), 200, "Thông báo");
    const found = ns.find((n) => n.refId === ctx.prId && n.title.startsWith("GV nhan xet"));
    if (!found) throw new Error("Sinh viên không nhận được thông báo nhận xét");
    return `thông báo #${found.id}`;
  });

  // 15 — Kanban
  await step("15", "Sinh viên đổi trạng thái Kanban", async () => {
    const r = expectStatus(await call("PATCH", `/progress/${ctx.prId}/status`, { token: ctx.student.token, body: { status: "IN_PROGRESS" } }), 200, "Đổi trạng thái");
    return `status=${r.status}`;
  });

  // 16 — Chat
  await step("16", "Chat giảng viên → sinh viên", async () => {
    const content = `Hẹn gặp thứ 5 #${RUN}`;
    expectStatus(await call("POST", "/messages/send", { token: ctx.lecturer.token, body: { recipientId: String(ctx.student.id), topicId: String(ctx.topicId), content } }), 200, "Gửi tin");
    const inbox = expectStatus(await call("GET", "/messages/inbox", { token: ctx.student.token }), 200, "Inbox");
    if (!inbox.some((m) => m.content === content)) throw new Error("Tin nhắn không có trong inbox sinh viên");
    return "sinh viên nhận được tin nhắn";
  });

  // 17 — Chấm điểm
  await step("17", "Giảng viên chấm điểm", async () => {
    const r = expectStatus(await call("POST", `/registration/grade/${ctx.reg1}`, { token: ctx.lecturer.token, body: { score: 8.5, feedback: "Tốt" } }), 200, "Chấm điểm");
    return `score=${r.score}`;
  });

  // 18 — Xuất Excel
  await step("18", "Giảng viên xuất Excel", async () => {
    const r = await call("GET", "/export/excel", { token: ctx.lecturer.token, query: { topicId: ctx.topicId } });
    expectStatus(r, 200, "Export");
    if (!r.type.includes("spreadsheetml")) throw new Error(`content-type sai: ${r.type}`);
    return `${r.data.length} bytes xlsx`;
  });

  // 19 — Khóa tài khoản (lỗi P1-02)
  await knownBug("19", "Admin khóa tài khoản → token cũ của sinh viên còn dùng được?", "P1-02", async () => {
    expectStatus(await call("PUT", `/users/${ctx.student.id}/status`, { token: ctx.admin.token, query: { active: false } }), 200, "Khóa");
    const r = await call("GET", "/registration/mine", { token: ctx.student.token, query: { studentId: ctx.student.id } });
    expectStatus(await call("PUT", `/users/${ctx.student.id}/status`, { token: ctx.admin.token, query: { active: true } }), 200, "Mở khóa lại");
    return { bug: r.status === 200, detail: `sau khi khóa: HTTP ${r.status} (đã mở khóa lại tài khoản)` };
  });

  // 20 — Quên mật khẩu (P2-05)
  await knownBug("20", "Chức năng Quên mật khẩu", "P2-05", async () => {
    const r = await call("POST", "/auth/password/forgot", { body: { identifier: "student" } });
    return { bug: r.status === 404 || r.status === 405 || r.status === 401, detail: `POST /auth/password/forgot → HTTP ${r.status}` };
  });

  // 21 — Upload > 1MB qua nginx (P2-03)
  await knownBug("21", "Upload file 2MB qua nginx (cổng frontend)", "P2-03", async () => {
    const form = new FormData();
    form.append("file", new Blob([Buffer.alloc(2 * 1024 * 1024, 65)], { type: "application/pdf" }), `big-${RUN}.pdf`);
    const r = await call("POST", "/api/upload", { base: WEB, token: ctx.student.token, form });
    return { bug: r.status === 413, detail: `HTTP ${r.status}` };
  });

  // ── Thăm dò bảo mật (bằng chứng cho Giai đoạn 1) ──
  await knownBug("S1", "Sinh viên đọc toàn bộ audit log", "P1-05 #12", async () => {
    const r = await call("GET", "/audit/recent", { token: ctx.student.token });
    return { bug: r.status === 200, detail: `HTTP ${r.status}${Array.isArray(r.data) ? `, ${r.data.length} bản ghi` : ""}` };
  });
  await knownBug("S2", "Giảng viên khác tạo milestone trên đề tài không phải của mình", "P1-05 #1", async () => {
    const other = await login(ctx.lecturer2Name);
    const r = await call("POST", "/milestones", { token: other.token, body: { topicId: ctx.topicId, title: "Milestone lạ", deadline: "2030-01-01T00:00:00" } });
    if (r.status === 200) await call("DELETE", `/milestones/${r.data.id}`, { token: other.token });
    return { bug: r.status === 200, detail: `HTTP ${r.status}` };
  });
  await knownBug("S3", "Sinh viên xem email/SĐT của người khác qua /users/{id}", "P1-05 #13", async () => {
    const r = await call("GET", `/users/${ctx.lecturer.id}`, { token: ctx.student.token });
    return { bug: r.status === 200 && !!r.data?.email, detail: `HTTP ${r.status}, email=${r.data?.email ?? "—"}` };
  });
  await knownBug("S4", "Tự đăng ký tài khoản công khai /auth/register", "P1-09", async () => {
    const r = await call("POST", "/auth/register", { body: { username: `self${RUN}`, password: "1", fullName: "Tự đăng ký", studentCode: `X${RUN}`, className: "X" } });
    return { bug: r.status === 200, detail: `HTTP ${r.status}${r.status === 200 ? " — tạo được tài khoản với mật khẩu '1'" : ""}` };
  });
}

// ───────────────────────── tổng kết ─────────────────────────

function summary() {
  const count = (s) => results.filter((r) => r.status === s).length;
  const line = `OK=${count("OK")}  BUG(đã biết)=${count("BUG")}  FIXED=${count("FIXED")}  FAIL=${count("FAIL")}  — tổng ${results.length}`;
  console.log(`\n${"─".repeat(70)}\n${line}\n`);
  if (process.env.REPORT_FILE) {
    const md = [
      `# Kết quả smoke test — ${new Date().toISOString()}`,
      "",
      `API: \`${API}\` · WEB: \`${WEB}\` · ${line}`,
      "",
      "| # | Kịch bản | Kết quả | Task | Chi tiết |",
      "|---|---|:---:|---|---|",
      ...results.map((r) => `| ${r.id} | ${r.name} | ${r.status} | ${r.task} | ${String(r.detail).replace(/\|/g, "\\|")} |`),
      "",
    ].join("\n");
    writeFileSync(process.env.REPORT_FILE, md, "utf8");
    console.log(`Đã ghi báo cáo: ${process.env.REPORT_FILE}`);
  }
  process.exit(count("FAIL") > 0 ? 1 : 0);
}

main().catch((e) => record("X", "Lỗi không mong đợi", "FAIL", e.stack || e.message)).finally(summary);
