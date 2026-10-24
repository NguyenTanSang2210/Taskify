import { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Modal from "./Modal";

function Harness() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>Mở</button>
      {open && (
        <Modal title="Thêm sự kiện" onClose={() => setOpen(false)}>
          <input aria-label="Tiêu đề" data-autofocus />
          <button type="button">Lưu</button>
        </Modal>
      )}
    </>
  );
}

describe("Modal (P4-11)", () => {
  it("là hộp thoại có tên, focus vào ô data-autofocus khi mở", () => {
    render(<Harness />);
    fireEvent.click(screen.getByText("Mở"));
    const dialog = screen.getByRole("dialog", { name: "Thêm sự kiện" });
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(screen.getByLabelText("Tiêu đề")).toHaveFocus();
  });

  it("Esc đóng hộp thoại và trả focus về nút đã mở", () => {
    render(<Harness />);
    const opener = screen.getByText("Mở");
    opener.focus();
    fireEvent.click(opener);
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(opener).toHaveFocus();
  });

  it("Tab ở phần tử cuối quay về phần tử đầu", () => {
    const onClose = vi.fn();
    render(
      <Modal title="Hộp thoại" onClose={onClose}>
        <button type="button">Cuối</button>
      </Modal>
    );
    const close = screen.getByLabelText("Đóng");
    const last = screen.getByText("Cuối");
    last.focus();
    fireEvent.keyDown(document, { key: "Tab" });
    expect(close).toHaveFocus();
    fireEvent.click(close);
    expect(onClose).toHaveBeenCalled();
  });
});
