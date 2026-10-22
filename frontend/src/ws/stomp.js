import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";

// Luôn dùng đường dẫn tương đối "/ws" (Vite proxy khi dev, nginx khi chạy Docker).
const WS_ENDPOINT = "/ws";

export function createStompClient() {
  const token = (() => {
    try {
      return localStorage.getItem("token");
    } catch {
      return null;
    }
  })();
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_ENDPOINT),
    reconnectDelay: 3000,
    debug: () => {},
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
  });
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
  // Máy chủ từ chối (token sai/hết hạn, không đủ quyền subscribe): kết nối lại cũng vô ích,
  // nên dừng hẳn thay vì thử lại mỗi 3 giây. Mất mạng (onWebSocketClose) vẫn tự kết nối lại.
  client.onStompError = () => {
    if (typeof onStatus === "function") onStatus("error");
    client.deactivate();
  };
  client.onWebSocketClose = () => {
    if (typeof onStatus === "function") onStatus("disconnected");
  };
  if (typeof onStatus === "function") onStatus("connecting");
  client.activate();
  return client;
}
