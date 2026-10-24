import { useState } from "react";
import { STATUS_CONFIG } from "./kanbanConfig";
import KanbanCard from "./KanbanCard";

export default function KanbanColumn({ status, cards, onMove, updatingId }) {
  const cfg = STATUS_CONFIG[status];
  const [dragOver, setDragOver] = useState(false);

  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const id = Number(e.dataTransfer.getData("progressId"));
    const currentStatus = e.dataTransfer.getData("currentStatus");
    if (id && currentStatus !== status) {
      onMove(id, status);
    }
  };

  return (
    <div
      onDragOver={handleDragOver}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      className={`
        flex flex-col rounded-[1.75rem] border-2 transition-all duration-200 min-h-[400px]
        ${dragOver
          ? `${cfg.border} bg-primary/5 shadow-lg shadow-primary/10 scale-[1.01]`
          : "border-transparent bg-surface-container-low/50"
        }
      `}
    >
      {/* Column Header */}
      <div className={`flex items-center justify-between px-5 py-4 rounded-2xl mb-3 ${cfg.headerBg}`}>
        <div className="flex items-center gap-3">
          <div className={`w-2.5 h-2.5 rounded-full ${cfg.dot}`} />
          <span className={`text-[11px] font-black uppercase tracking-widest ${cfg.color}`}>
            {cfg.label}
          </span>
        </div>
        <span className={`text-[10px] font-black px-2.5 py-0.5 rounded-full ${cfg.badge}`}>
          {cards.length}
        </span>
      </div>

      {/* Drop zone visual */}
      {dragOver && (
        <div className={`mx-2 mb-3 rounded-xl border-2 border-dashed ${cfg.border} h-20 flex items-center justify-center`}>
          <span className={`text-[10px] font-black uppercase tracking-widest ${cfg.color}`}>
            Thả vào đây
          </span>
        </div>
      )}

      {/* Cards */}
      <div className="flex-1 px-2 pb-4 space-y-3 overflow-y-auto">
        {cards.length === 0 && !dragOver ? (
          <div className="flex flex-col items-center justify-center h-40 text-outline [&_.material-symbols-outlined]:opacity-40">
            <span className={`material-symbols-outlined text-4xl ${cfg.color}`}>{cfg.icon}</span>
            <p className="text-[10px] font-black uppercase tracking-widest text-outline mt-2">Trống</p>
          </div>
        ) : (
          cards.map((item) => (
            <KanbanCard
              key={item.id}
              item={item}
              onMove={onMove}
              updating={updatingId === item.id}
            />
          ))
        )}
      </div>
    </div>
  );
}
