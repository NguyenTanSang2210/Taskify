import axios from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import api from "./api";
import { getAccessToken, setAccessToken } from "./auth/session";
import { TOAST_EVENT } from "./toast";
import { stubLocation } from "./test/location";

/** Thay adapter của axios: mỗi request trả theo hàm `respond(config)` → { status, data }. */
function fakeServer(respond) {
  const calls = [];
  api.defaults.adapter = async (config) => {
    calls.push(config);
    const { status, data } = await respond(config, calls.length);
    const response = { status, data, headers: {}, config, statusText: String(status) };
    if (status >= 200 && status < 300) return response;
    const error = new axios.AxiosError("Request failed", "ERR", config, null, response);
    throw error;
  };
  return calls;
}

describe("api.js — interceptor", () => {
  let location;
  let toasts;
  const onToast = (e) => toasts.push(e.detail);

  beforeEach(() => {
    location = stubLocation("/notifications");
    toasts = [];
    window.addEventListener(TOAST_EVENT, onToast);
  });
  afterEach(() => {
    window.removeEventListener(TOAST_EVENT, onToast);
    setAccessToken(null);
  });

  it("gắn access token trong bộ nhớ vào header Authorization", async () => {
    setAccessToken("tok-1");
    const calls = fakeServer(() => ({ status: 200, data: { ok: true } }));
    await api.get("/users/1");
    expect(calls[0].headers.Authorization).toBe("Bearer tok-1");
  });

  it("401 → làm mới phiên một lần rồi gửi lại request với token mới (P3-14)", async () => {
    setAccessToken("het-han");
    vi.spyOn(axios, "post").mockResolvedValue({ data: { token: "moi" } });
    const calls = fakeServer((config, n) => (n === 1 ? { status: 401, data: {} } : { status: 200, data: { items: [] } }));
    const res = await api.get("/notifications/mine");
    expect(res.data).toEqual({ items: [] });
    expect(calls).toHaveLength(2);
    expect(calls[1].headers.Authorization).toBe("Bearer moi");
    expect(location.assign).not.toHaveBeenCalled();
  });

  it("vẫn 401 sau khi làm mới → thu hồi phiên và về trang đăng nhập kèm lý do", async () => {
    const post = vi.spyOn(axios, "post").mockResolvedValue({ data: { token: "moi" } });
    fakeServer(() => ({ status: 401, data: { message: "Phiên đăng nhập không hợp lệ" } }));
    await expect(api.get("/users")).rejects.toMatchObject({ status: 401 });
    await vi.waitFor(() => expect(location.assign).toHaveBeenCalledWith("/login?reason=expired"));
    expect(post).toHaveBeenCalledWith("/api/auth/logout", null, { withCredentials: true });
    expect(getAccessToken()).toBeNull();
  });

  it("403 chỉ báo lỗi (toast), KHÔNG đăng xuất", async () => {
    fakeServer(() => ({ status: 403, data: { message: "Forbidden" } }));
    await expect(api.get("/audit")).rejects.toMatchObject({ status: 403 });
    expect(toasts).toEqual([{ type: "warning", message: "Bạn không có quyền thực hiện thao tác này." }]);
    expect(location.assign).not.toHaveBeenCalled();
  });

  it("lỗi kiểm tra dữ liệu mang code + fieldErrors + thông báo tiếng Việt (P3-03)", async () => {
    fakeServer(() => ({
      status: 400,
      data: { code: "VALIDATION_FAILED", message: "Tiêu đề không được để trống",
        errors: [{ field: "title", message: "Tiêu đề không được để trống" }] },
    }));
    const err = await api.post("/milestones", {}).catch((e) => e);
    expect(err.message).toBe("Tiêu đề không được để trống");
    expect(err.code).toBe("VALIDATION_FAILED");
    expect(err.fieldErrors).toEqual({ title: "Tiêu đề không được để trống" });
    expect(toasts).toEqual([]);
  });

  it("lỗi máy chủ 500 → toast chung", async () => {
    fakeServer(() => ({ status: 500, data: {} }));
    await expect(api.get("/topics")).rejects.toMatchObject({ status: 500 });
    expect(toasts[0]).toEqual({ type: "danger", message: "Máy chủ gặp lỗi, vui lòng thử lại sau." });
  });

  it("endpoint xác thực bị 401 (sai mật khẩu) không làm mới phiên, không chuyển trang", async () => {
    const post = vi.spyOn(axios, "post");
    fakeServer(() => ({ status: 401, data: { message: "Sai tên đăng nhập hoặc mật khẩu" } }));
    await expect(api.post("/auth/login", {})).rejects.toMatchObject({ message: "Sai tên đăng nhập hoặc mật khẩu" });
    expect(post).not.toHaveBeenCalled();
    expect(location.assign).not.toHaveBeenCalled();
  });
});
