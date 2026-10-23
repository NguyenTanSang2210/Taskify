import { describe, expect, it } from "vitest";
import { submissionState, toLocalInput } from "./milestoneUtils";

const now = new Date("2026-10-20T12:00:00");

describe("submissionState", () => {
    it("phân biệt đúng hạn, nộp trễ, chưa nộp và quá hạn chưa nộp", () => {
        expect(submissionState({ submittedAt: "2026-10-10T09:00:00", late: false }, { deadline: "2026-10-11T00:00:00" }, now).label).toBe("Đúng hạn");
        expect(submissionState({ submittedAt: "2026-10-12T09:00:00", late: true }, { deadline: "2026-10-11T00:00:00" }, now).label).toBe("Nộp trễ");
        expect(submissionState({ submittedAt: null }, { deadline: "2026-10-25T00:00:00" }, now).label).toBe("Chưa nộp");
        expect(submissionState({ submittedAt: null }, { deadline: "2026-10-11T00:00:00" }, now).label).toBe("Quá hạn, chưa nộp");
    });
});

describe("toLocalInput", () => {
    it("cắt ISO về dạng datetime-local", () => {
        expect(toLocalInput("2026-11-02T10:30:00")).toBe("2026-11-02T10:30");
        expect(toLocalInput(null)).toBe("");
    });
});
