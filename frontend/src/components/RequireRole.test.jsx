import axios from "axios";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RequireRole from "./RequireRole";
import { AuthProvider } from "../context/AuthContext";

function renderAt(role, sessionRole) {
  if (sessionRole) {
    vi.spyOn(axios, "post").mockResolvedValue({ data: { token: "t", userId: 1, username: "u", fullName: "U", role: sessionRole } });
  } else {
    vi.spyOn(axios, "post").mockRejectedValue(new Error("401"));
  }
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={["/admin"]}>
        <Routes>
          <Route path="/admin" element={<RequireRole role={role}><p>Trang quản trị</p></RequireRole>} />
          <Route path="/login" element={<p>Trang đăng nhập</p>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  );
}

describe("RequireRole", () => {
  beforeEach(() => localStorage.clear());

  it("đúng vai trò → hiện nội dung", async () => {
    renderAt(["ADMIN", "DEPARTMENT_ADMIN"], "ADMIN");
    expect(await screen.findByText("Trang quản trị")).toBeInTheDocument();
  });

  it("sai vai trò hoặc chưa đăng nhập → về trang đăng nhập", async () => {
    renderAt("ADMIN", "STUDENT");
    expect(await screen.findByText("Trang đăng nhập")).toBeInTheDocument();
  });
});
