import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { getAccessToken, onAccessTokenChange, refreshSession, revokeSession, setAccessToken, tokenExpiresAt } from "./session";

const jwt = (payload) => `x.${btoa(JSON.stringify(payload)).replace(/=+$/, "")}.y`;

describe("auth/session (P3-14)", () => {
  afterEach(() => setAccessToken(null));

  it("giữ access token trong bộ nhớ, không ghi localStorage, và báo cho người nghe", () => {
    const seen = [];
    const off = onAccessTokenChange((t) => seen.push(t));
    setAccessToken("abc");
    off();
    setAccessToken("def");
    expect(getAccessToken()).toBe("def");
    expect(seen).toEqual(["abc"]);
    expect(Object.keys(localStorage)).toHaveLength(0);
  });

  it("nhiều request cùng hết hạn chỉ gọi /auth/refresh MỘT lần", async () => {
    let resolve;
    const post = vi.spyOn(axios, "post").mockReturnValue(new Promise((r) => { resolve = r; }));
    const a = refreshSession();
    const b = refreshSession();
    resolve({ data: { token: "moi", userId: 1 } });
    await expect(a).resolves.toMatchObject({ token: "moi" });
    await expect(b).resolves.toMatchObject({ token: "moi" });
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith("/api/auth/refresh", null, { withCredentials: true });
    expect(getAccessToken()).toBe("moi");
  });

  it("làm mới thất bại → xóa token và ném lỗi", async () => {
    setAccessToken("cu");
    vi.spyOn(axios, "post").mockRejectedValue(new Error("401"));
    await expect(refreshSession()).rejects.toThrow("401");
    expect(getAccessToken()).toBeNull();
  });

  it("đăng xuất vẫn xóa token phía trình duyệt kể cả khi lỗi mạng", async () => {
    setAccessToken("cu");
    const post = vi.spyOn(axios, "post").mockRejectedValue(new Error("mất mạng"));
    await revokeSession();
    expect(post).toHaveBeenCalledWith("/api/auth/logout", null, { withCredentials: true });
    expect(getAccessToken()).toBeNull();
  });

  it("đọc thời điểm hết hạn từ claim exp; token hỏng → null", () => {
    expect(tokenExpiresAt(jwt({ exp: 1700000000 }))).toBe(1700000000 * 1000);
    expect(tokenExpiresAt("khong-phai-jwt")).toBeNull();
    expect(tokenExpiresAt(jwt({ sub: "a" }))).toBeNull();
  });
});
