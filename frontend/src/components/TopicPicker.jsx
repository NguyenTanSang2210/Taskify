/** Chọn một trong các đề tài của người dùng; chỉ hiện khi có từ 2 đề tài trở lên. */
export default function TopicPicker({ topics, value, onChange, id = "topic-picker" }) {
  if (topics.length < 2) return null;
  return (
    <div className="flex items-center gap-2">
      <label htmlFor={id} className="text-[10px] font-black uppercase tracking-widest text-outline whitespace-nowrap">Đề tài</label>
      <select id={id} value={value} onChange={(e) => onChange(e.target.value)}
        className="max-w-[18rem] px-3 py-2 bg-white dark:bg-slate-900 border border-outline-variant/20 rounded-xl text-sm font-bold outline-none focus:border-primary">
        {topics.map((t) => (
          <option key={t.id} value={String(t.id)}>{t.title}</option>
        ))}
      </select>
    </div>
  );
}
