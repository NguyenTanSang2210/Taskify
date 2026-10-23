import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Pagination from "./Pagination";

describe("Pagination (P3-06)", () => {
  it("một trang: chỉ hiện tổng số mục, không có nút", () => {
    render(<Pagination page={0} totalPages={1} totalElements={7} onChange={() => {}} />);
    expect(screen.getByText("7 mục")).toBeInTheDocument();
    expect(screen.queryByRole("button")).toBeNull();
  });

  it("trang đầu: không lùi được; bấm 'Trang sau' gọi onChange(page + 1)", () => {
    const onChange = vi.fn();
    render(<Pagination page={0} totalPages={3} totalElements={50} onChange={onChange} />);
    expect(screen.getByText(/Trang 1\/3/)).toBeInTheDocument();
    expect(screen.getByLabelText("Trang trước")).toBeDisabled();
    fireEvent.click(screen.getByLabelText("Trang sau"));
    expect(onChange).toHaveBeenCalledWith(1);
  });

  it("trang cuối: không tiến được; đang tải thì khóa cả hai nút", () => {
    const { rerender } = render(<Pagination page={2} totalPages={3} onChange={() => {}} />);
    expect(screen.getByLabelText("Trang sau")).toBeDisabled();
    expect(screen.getByLabelText("Trang trước")).toBeEnabled();
    rerender(<Pagination page={1} totalPages={3} onChange={() => {}} disabled />);
    expect(screen.getByLabelText("Trang trước")).toBeDisabled();
    expect(screen.getByLabelText("Trang sau")).toBeDisabled();
  });
});
