import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";

export function createStompClient() {
  const token = (() => {
    try {
      return localStorage.getItem("token");
    } catch {
      return null;
    }
  })();
  const client = new Client({
    webSocketFactory: () => new SockJS("/ws"),
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
