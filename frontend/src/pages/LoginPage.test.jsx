import axios from "axios";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import LoginPage from "./LoginPage";
import api from "../api";
import { AuthProvider } from "../context/AuthContext";
import { getAccessToken, setAccessToken } from "../auth/session";

function serve(routes) {
  const calls = [];
  api.defaults.adapter = async (config) => {
    calls.push(config);
    const handler = routes[config.url];
    const { status = 200, data } = handler ? handler(JSON.parse(config.data || "{}")) : { status: 404, data: {} };
    const response = { status, data, headers: {}, config, statusText: "" };
    if (status < 300) return response;
    throw new axios.AxiosError("fail", "ERR", config, null, response);
  };
  return calls;
}

const renderLogin = () =>
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={["/login"]}>
        <LoginPage />
      </MemoryRouter>
    </AuthProvider>
  );

describe("LoginPage", () => {
  // (thân hàm dạng khối: giá trị trả về của beforeEach bị Vitest coi là hàm dọn dẹp)
  beforeEach(() => {
    vi.spyOn(axios, "post").mockRejectedValue(new Error("chưa có phiên"));
  });
  afterEach(() => setAccessToken(null));

  it("vai trò cần OTP: dừng ở bước OTP, chưa có token nào; nhập đúng OTP thì vào hệ thống", async () => {
    const calls = serve({
      "/auth/login": () => ({ data: { nextStep: "OTP", preAuthToken: "pre-1", maskedEmail: "a***@x.vn", resendCooldownSeconds: 60 } }),
      "/auth/otp/verify": (body) => (body.code === "123456"
        ? { data: { nextStep: "NONE", token: "acc-1", userId: 3, username: "admin", fullName: "Quản trị", role: "ADMIN" } }
        : { status: 401, data: { message: "Mã OTP không đúng" } }),
    });
    renderLogin();
    fireEvent.change(screen.getByPlaceholderText("Nhập mã sinh viên hoặc username"), { target: { value: "admin" } });
    fireEvent.change(screen.getByPlaceholderText("••••••••"), { target: { value: "Passw0rd" } });
    fireEvent.submit(screen.getByPlaceholderText("••••••••").closest("form"));

    expect(await screen.findByText("Xác thực OTP")).toBeInTheDocument();
    expect(screen.getByText("a***@x.vn")).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();

    fireEvent.change(screen.getByPlaceholderText("000000"), { target: { value: "123456" } });
    fireEvent.submit(screen.getByPlaceholderText("000000").closest("form"));
    await vi.waitFor(() => expect(getAccessToken()).toBe("acc-1"));
    expect(calls.find((c) => c.url === "/auth/otp/verify").data).toBe(JSON.stringify({ preAuthToken: "pre-1", code: "123456" }));
  });

  it("sai mật khẩu: hiện thông báo của máy chủ, ở lại trang đăng nhập", async () => {
    serve({ "/auth/login": () => ({ status: 401, data: { message: "Tên đăng nhập hoặc mật khẩu không đúng" } }) });
    renderLogin();
    fireEvent.change(screen.getByPlaceholderText("Nhập mã sinh viên hoặc username"), { target: { value: "sv" } });
    fireEvent.change(screen.getByPlaceholderText("••••••••"), { target: { value: "sai" } });
    fireEvent.submit(screen.getByPlaceholderText("••••••••").closest("form"));
    expect(await screen.findByText(/Tên đăng nhập hoặc mật khẩu không đúng/)).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });
});
