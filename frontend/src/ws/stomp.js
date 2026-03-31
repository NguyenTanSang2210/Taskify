import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";

const IS_BROWSER = typeof window !== "undefined";
const IS_LOCALHOST = IS_BROWSER
  ? window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
  : false;
const IS_FRONTEND_DEV_PORT = IS_BROWSER && window.location.port === "5173";
const WS_ENDPOINT = IS_LOCALHOST && IS_FRONTEND_DEV_PORT ? "http://localhost:8080/ws" : "/ws";

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
  client.onStompError = () => {
    if (typeof onStatus === "function") onStatus("error");
  };
  client.onWebSocketClose = () => {
    if (typeof onStatus === "function") onStatus("disconnected");
  };
  if (typeof onStatus === "function") onStatus("connecting");
  client.activate();
  return client;
}
