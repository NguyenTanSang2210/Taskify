import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";
import { getAccessToken, refreshSession } from "../auth/session";

// Luôn dùng đường dẫn tương đối "/ws" (Vite proxy khi dev, nginx khi chạy Docker).
const WS_ENDPOINT = "/ws";

export function createStompClient() {
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_ENDPOINT),
    reconnectDelay: 3000,
    debug: () => {},
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });
  // Mỗi lần (kết nối lại) lấy access token MỚI NHẤT trong bộ nhớ (token 15 phút được làm mới định kỳ — P3-14)
  client.beforeConnect = () => {
    const token = getAccessToken();
    client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
  };
  return client;
}

export function connectAndSubscribe({ destination, onMessage, onStatus }) {
  const client = createStompClient();
  client.onConnect = () => {
    if (typeof onStatus === "function") onStatus("connected");
    client.subscribe(destination, (msg) => {
      try {
        const body = JSON.parse(msg.body);
        onMessage(body);
      } catch {
        onMessage(msg.body);
      }
    });
  };
  // Máy chủ từ chối: có thể do access token vừa hết hạn → làm mới phiên và thử lại MỘT lần; vẫn lỗi (không đủ quyền
  // subscribe...) thì dừng hẳn thay vì thử lại mỗi 3 giây. Mất mạng (onWebSocketClose) vẫn tự kết nối lại.
  let retriedAfterRefresh = false;
  client.onStompError = async () => {
    if (typeof onStatus === "function") onStatus("error");
    await client.deactivate();
    if (retriedAfterRefresh) return;
    retriedAfterRefresh = true;
    try {
      await refreshSession();
      client.activate();
    } catch { /* phiên hết hạn: api.js sẽ đưa về trang đăng nhập ở request kế tiếp */ }
  };
  client.onWebSocketClose = () => {
    if (typeof onStatus === "function") onStatus("disconnected");
  };
  if (typeof onStatus === "function") onStatus("connecting");
  client.activate();
  return client;
}
