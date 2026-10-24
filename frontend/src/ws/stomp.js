import { getAccessToken, refreshSession } from "../auth/session";

// Luôn dùng đường dẫn tương đối "/ws" (Vite proxy khi dev, nginx khi chạy Docker).
const WS_ENDPOINT = "/ws";

// P3-11: thư viện STOMP + SockJS (~84 KB) tải riêng khi lần đầu kết nối, không nằm trong bundle ban đầu
let libs = null;
const loadLibs = () => {
  libs ??= Promise.all([import("@stomp/stompjs"), import("sockjs-client/dist/sockjs")]).then(([stomp, sockjs]) => ({
    Client: stomp.Client,
    SockJS: sockjs.default ?? sockjs,
  }));
  return libs;
};

function createStompClient({ Client, SockJS }) {
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

/**
 * Đăng ký nhận tin từ một kênh. Trả về handle có deactivate() — gọi được ngay cả khi thư viện chưa tải xong
 * (component unmount sớm): khi đó sẽ không kết nối nữa.
 */
export function connectAndSubscribe({ destination, onMessage, onStatus }) {
  let client = null;
  let stopped = false;
  const status = (s) => typeof onStatus === "function" && onStatus(s);

  status("connecting");
  loadLibs()
    .then((loaded) => {
      if (stopped) return;
      client = createStompClient(loaded);
      client.onConnect = () => {
        status("connected");
        client.subscribe(destination, (msg) => {
          try {
            onMessage(JSON.parse(msg.body));
          } catch {
            onMessage(msg.body);
          }
        });
      };
      // Máy chủ từ chối: có thể do access token vừa hết hạn → làm mới phiên và thử lại MỘT lần; vẫn lỗi (không đủ
      // quyền subscribe...) thì dừng hẳn thay vì thử lại mỗi 3 giây. Mất mạng (onWebSocketClose) vẫn tự kết nối lại.
      let retriedAfterRefresh = false;
      client.onStompError = async () => {
        status("error");
        await client.deactivate();
        if (retriedAfterRefresh || stopped) return;
        retriedAfterRefresh = true;
        try {
          await refreshSession();
          if (!stopped) client.activate();
        } catch { /* phiên hết hạn: api.js sẽ đưa về trang đăng nhập ở request kế tiếp */ }
      };
      client.onWebSocketClose = () => status("disconnected");
      client.activate();
    })
    .catch(() => status("error"));

  return {
    deactivate: () => {
      stopped = true;
      return client?.deactivate();
    },
  };
}
