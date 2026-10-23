import axios from "axios";
import { act, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";
import { getAccessToken } from "../auth/session";

function Probe() {
  const { user, ready, logout } = useAuth();
  if (!ready) return <p>đang tải</p>;
  return (
    <div>
      <p>{user ? `xin chào ${user.fullName} (${user.role})` : "chưa đăng nhập"}</p>
      <button onClick={logout}>đăng xuất</button>
    </div>
  );
}

describe("AuthContext (P3-14)", () => {
  it("tải lại trang: khôi phục phiên từ cookie refresh, token chỉ trong bộ nhớ", async () => {
    localStorage.setItem("token", "token-cu-truoc-P3-14");
    vi.spyOn(axios, "post").mockResolvedValue({ data: { token: "moi", userId: 7, username: "sv", fullName: "Sinh Viên", role: "STUDENT" } });
    render(<AuthProvider><Probe /></AuthProvider>);
    expect(screen.getByText("đang tải")).toBeInTheDocument();
    expect(await screen.findByText("xin chào Sinh Viên (STUDENT)")).toBeInTheDocument();
    expect(getAccessToken()).toBe("moi");
    expect(localStorage.getItem("token")).toBeNull(); // dọn token kiểu cũ
    expect(JSON.parse(localStorage.getItem("user"))).toMatchObject({ id: 7, role: "STUDENT" });
  });

  it("không có phiên (refresh 401) → chưa đăng nhập", async () => {
    localStorage.setItem("user", JSON.stringify({ id: 1 }));
    vi.spyOn(axios, "post").mockRejectedValue(new Error("401"));
    render(<AuthProvider><Probe /></AuthProvider>);
    expect(await screen.findByText("chưa đăng nhập")).toBeInTheDocument();
    expect(localStorage.getItem("user")).toBeNull();
  });

  it("đăng xuất gọi /auth/logout và xóa phiên phía trình duyệt", async () => {
    const post = vi.spyOn(axios, "post").mockResolvedValue({ data: { token: "moi", userId: 7, username: "sv", fullName: "SV", role: "STUDENT" } });
    render(<AuthProvider><Probe /></AuthProvider>);
    await screen.findByText(/xin chào/);
    await act(async () => screen.getByText("đăng xuất").click());
    expect(await screen.findByText("chưa đăng nhập")).toBeInTheDocument();
    expect(post).toHaveBeenCalledWith("/api/auth/logout", null, { withCredentials: true });
    expect(getAccessToken()).toBeNull();
  });
});
