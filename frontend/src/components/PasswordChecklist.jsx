// Quy tắc mật khẩu — khớp với PasswordPolicy ở backend (8–64 ký tự, có chữ cái và chữ số, không chứa tên đăng nhập).
export function passwordRules(password, username = "") {
  const pw = password || "";
  const user = (username || "").trim().toLowerCase();
  return [
    { id: "len", label: "Từ 8 đến 64 ký tự", ok: pw.length >= 8 && pw.length <= 64 },
    { id: "letter", label: "Có ít nhất một chữ cái", ok: /\p{L}/u.test(pw) },
    { id: "digit", label: "Có ít nhất một chữ số", ok: /\p{N}/u.test(pw) },
    { id: "user", label: "Không chứa tên đăng nhập", ok: !user || !pw.toLowerCase().includes(user) },
  ];
}

export function isPasswordValid(password, username = "") {
  return passwordRules(password, username).every((r) => r.ok);
}

export default function PasswordChecklist({ password, username }) {
  const rules = passwordRules(password, username);
  return (
    <ul className="space-y-1 mt-2 ml-2" aria-label="Yêu cầu mật khẩu">
      {rules.map((r) => (
        <li
          key={r.id}
          className={`flex items-center gap-2 text-[11px] font-bold ${r.ok ? "text-green-700" : "text-outline"}`}
        >
          <span className="material-symbols-outlined text-base">{r.ok ? "check_circle" : "radio_button_unchecked"}</span>
          {r.label}
        </li>
      ))}
    </ul>
  );
}
