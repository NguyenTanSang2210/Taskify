#!/usr/bin/env node
/**
 * Kiểm tra bảo mật WebSocket (STOMP thô qua WebSocket của Node >= 22, không cần thư viện).
 *
 *   node scripts/ws-check.mjs
 *
 * Biến môi trường: API_URL (mặc định http://localhost:8081/api), WS_URL (mặc định ws://localhost:8081/ws/websocket),
 * PASSWORD (mặc định mật khẩu tài khoản demo).
 *
 * Kịch bản (mỗi mục in PASS/FAIL, thoát mã 1 nếu có FAIL):
 *   1. CONNECT không token           → bị từ chối (ERROR)
 *   2. CONNECT token rác             → bị từ chối
 *   3. CONNECT token hợp lệ          → CONNECTED
 *   4. SUBSCRIBE /topic/notifications.{id mình}, /topic/messages.{id mình} → được phép (không ERROR)
 *   5. SUBSCRIBE kênh riêng của người khác, kênh kiểu cũ (/topic/notifications/{id}, /user/queue/*), /topic/events → bị từ chối
 *   6. SUBSCRIBE /topic/progress.{đề tài không thuộc mình} → bị từ chối
 *   7. SUBSCRIBE /topic/registration.{id} (sinh viên)       → bị từ chối
 *   8. SEND lên /topic/...           → bị từ chối
 */
const API = process.env.API_URL || "http://localhost:8081/api";
const WS = process.env.WS_URL || "ws://localhost:8081/ws/websocket";
const PASSWORD = process.env.PASSWORD || "123456";

let failed = 0;
const report = (ok, name, detail = "") => {
  if (!ok) failed++;
  console.log(`${ok ? "✅ PASS" : "❌ FAIL"}  ${name}${detail ? " — " + detail : ""}`);
};

/** Mở một phiên STOMP; trả về { frames, send(), close(), waitFor(pred, ms) }. */
function openStomp(headers) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(WS, ["v12.stomp"]);
    const frames = [];
    const waiters = [];
    const session = {
      frames,
      send: (command, hdrs = {}, body = "") => {
        const head = Object.entries(hdrs).map(([k, v]) => `${k}:${v}`).join("\n");
        ws.send(`${command}\n${head}${head ? "\n" : ""}\n${body}\0`);
      },
      close: () => { try { ws.close(); } catch { /* noop */ } },
      waitFor: (pred, ms = 2000) =>
        new Promise((res) => {
          const hit = frames.find(pred);
          if (hit) return res(hit);
          const t = setTimeout(() => res(null), ms);
          waiters.push((f) => { if (pred(f)) { clearTimeout(t); res(f); return true; } return false; });
        }),
    };
    ws.onmessage = (ev) => {
      for (const raw of String(ev.data).split("\0")) {
        const text = raw.replace(/^\n+/, "");
        if (!text) continue;
        const [head, ...rest] = text.split("\n\n");
        const lines = head.split("\n");
        const frame = { command: lines[0], headers: Object.fromEntries(lines.slice(1).map((l) => l.split(/:(.*)/s).slice(0, 2))), body: rest.join("\n\n") };
        frames.push(frame);
        for (let i = waiters.length - 1; i >= 0; i--) if (waiters[i](frame)) waiters.splice(i, 1);
      }
    };
    ws.onopen = () => {
      session.send("CONNECT", { "accept-version": "1.2", "heart-beat": "0,0", ...headers });
      resolve(session);
    };
    ws.onerror = () => reject(new Error("Không mở được WebSocket tới " + WS));
  });
}

const isError = (f) => f.command === "ERROR";
const isConnected = (f) => f.command === "CONNECTED";

async function main() {
  const res = await fetch(`${API}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: "student", password: PASSWORD }),
  });
  const login = await res.json();
  if (!login.token) {
    console.error("Không đăng nhập được tài khoản student (đặt PASSWORD đúng?)", res.status, login);
    process.exit(2);
  }
  const userId = login.userId;

  // 1–2: không token / token rác
  for (const [label, hdrs] of [["không token", {}], ["token rác", { Authorization: "Bearer khong.phai.jwt" }]]) {
    const s = await openStomp(hdrs);
    const f = await s.waitFor((x) => isError(x) || isConnected(x));
    report(f && isError(f), `CONNECT ${label} bị từ chối`, f ? f.command : "không phản hồi");
    s.close();
  }

  // 3: token hợp lệ
  const s = await openStomp({ Authorization: `Bearer ${login.token}` });
  const connected = await s.waitFor(isConnected);
  report(!!connected, "CONNECT token hợp lệ được chấp nhận");
  if (!connected) { s.close(); process.exit(1); }

  // Mỗi lần thử trên một phiên MỚI (server đóng phiên sau ERROR)
  const tryOnce = async (action) => {
    const c = await openStomp({ Authorization: `Bearer ${login.token}` });
    await c.waitFor(isConnected);
    action(c);
    const err = await c.waitFor(isError, 1500);
    c.close();
    return !!err;
  };

  // 4: kênh riêng của chính mình được phép (P4-10: /topic/{notifications|messages}.{userId})
  for (const dest of [`/topic/notifications.${userId}`, `/topic/messages.${userId}`]) {
    const denied = await tryOnce((c) => c.send("SUBSCRIBE", { id: "s1", destination: dest }));
    report(!denied, `SUBSCRIBE ${dest} được phép`);
  }

  // 5: kênh riêng của người khác, kênh kiểu cũ + kênh sự kiện chung
  for (const dest of [`/topic/notifications.${userId + 1}`, `/topic/messages.${userId + 1}`, `/topic/notifications/${userId}`,
    "/user/queue/notifications", "/topic/events"]) {
    const denied = await tryOnce((c) => c.send("SUBSCRIBE", { id: "s1", destination: dest }));
    report(denied, `SUBSCRIBE ${dest} bị từ chối`);
  }

  // 6: tiến độ của đề tài không thuộc mình (id rất lớn → không tồn tại / không phải thành viên)
  {
    const denied = await tryOnce((c) => c.send("SUBSCRIBE", { id: "s1", destination: "/topic/progress.999999" }));
    report(denied, "SUBSCRIBE /topic/progress.{đề tài lạ} bị từ chối");
  }

  // 7: sinh viên không được nghe kênh đăng ký
  {
    const denied = await tryOnce((c) => c.send("SUBSCRIBE", { id: "s1", destination: "/topic/registration.1" }));
    report(denied, "SUBSCRIBE /topic/registration.{id} (sinh viên) bị từ chối");
  }

  // 8: SEND
  {
    const denied = await tryOnce((c) => c.send("SEND", { destination: "/topic/progress.1", "content-type": "application/json" }, "{\"type\":\"FAKE\"}"));
    report(denied, "SEND từ client bị từ chối");
  }

  // 9: chiều tích cực — thông báo thật sự đến đúng người qua /user/queue/notifications.
  // Sinh viên nộp báo cáo → hệ thống tạo thông báo "Đã gửi báo cáo" cho chính sinh viên đó.
  {
    const auth = { Authorization: `Bearer ${login.token}`, "Content-Type": "application/json" };
    const regs = await (await fetch(`${API}/registration/mine?studentId=${userId}`, { headers: auth })).json();
    const approved = (Array.isArray(regs) ? regs : []).filter((r) => r.approved === true && r.topic?.id);
    let delivered = null;
    for (const reg of approved.sort((a, b) => b.topic.id - a.topic.id)) {
      const listener = await openStomp({ Authorization: `Bearer ${login.token}` });
      await listener.waitFor(isConnected);
      listener.send("SUBSCRIBE", { id: "n1", destination: `/topic/notifications.${userId}` });
      const make = await fetch(`${API}/progress/create?studentId=${userId}&topicId=${reg.topic.id}`, {
        method: "POST", headers: auth, body: JSON.stringify({ title: `ws-check ${Date.now()}`, content: "kiểm tra realtime" }),
      });
      if (make.ok) {
        const msg = await listener.waitFor((f) => f.command === "MESSAGE", 3000);
        delivered = msg ? JSON.parse(msg.body) : false;
        listener.close();
        break;
      }
      listener.close();
    }
    if (delivered === null) console.log("⏭️  SKIP  nhận thông báo realtime — không có đề tài nào cho phép nộp báo cáo");
    else report(!!delivered?.notification?.id && delivered.notification.user === undefined,
      "Nhận thông báo realtime qua kênh riêng /topic/notifications.{id} (không lộ user)");
  }

  s.close();
  console.log(failed === 0 ? "\nTất cả kiểm tra WebSocket đều đạt." : `\n${failed} kiểm tra thất bại.`);
  process.exit(failed === 0 ? 0 : 1);
}

main().catch((e) => { console.error(e); process.exit(2); });
