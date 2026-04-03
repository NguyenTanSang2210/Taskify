import React from "react";

export default function TopicCard({ topic, onRegister, loading }) {
  const isOpen = topic.status === "OPEN";
  const registrationStatus = topic.registrationStatus;

  const renderActionButton = () => {
    if (registrationStatus === "CHO_DUYET") {
      return (
        <span className="px-4 py-2 bg-amber-500/10 text-amber-600 text-[10px] font-black rounded-xl uppercase tracking-widest border border-amber-500/10 flex items-center gap-2">
          <span className="material-symbols-outlined text-sm">pending</span>
          Đang chờ duyệt
        </span>
      );
    }
    if (registrationStatus === "DA_DUYET") {
      return (
        <span className="px-4 py-2 bg-green-500/10 text-green-600 text-[10px] font-black rounded-xl uppercase tracking-widest border border-green-500/10 flex items-center gap-2">
          <span className="material-symbols-outlined text-sm">verified</span>
          Đã được duyệt
        </span>
      );
    }
    if (registrationStatus === "TU_CHOI") {
      return (
        <span className="px-4 py-2 bg-error/10 text-error text-[10px] font-black rounded-xl uppercase tracking-widest border border-error/10 flex items-center gap-2">
          <span className="material-symbols-outlined text-sm">cancel</span>
          Đã bị từ chối
        </span>
      );
    }

    return (
      <button
        className={`px-6 py-2.5 rounded-xl font-black text-[10px] uppercase tracking-[0.2em] transition-all duration-300 flex items-center gap-2 ${
          !isOpen || loading
            ? "bg-surface-container text-outline/40 cursor-not-allowed border border-outline-variant/10"
            : "bg-primary text-white shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 hover:shadow-primary/30"
        }`}
        disabled={!isOpen || loading}
        onClick={() => onRegister(topic.id)}
      >
        {loading ? (
           <span className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
        ) : (
           <span className="material-symbols-outlined text-sm">how_to_reg</span>
        )}
        {loading ? "Xử lý..." : "Đăng ký đề tài"}
      </button>
    );
  };

  return (
    <div className="bg-white dark:bg-slate-900 p-8 rounded-[2.5rem] border border-outline-variant/10 hover:border-primary/20 hover:shadow-2xl hover:shadow-primary/5 transition-all duration-500 flex flex-col group relative overflow-hidden h-full">
      {/* Visual background element */}
      <div className="absolute -top-12 -right-12 w-32 h-32 bg-primary/5 rounded-full blur-3xl group-hover:bg-primary/10 transition-all duration-500"></div>
      
      <div className="flex justify-between items-start mb-6 relative z-10">
        <div className="flex items-center gap-2">
           <span className="px-3 py-1 bg-surface-container text-outline text-[9px] font-black rounded-lg uppercase tracking-widest border border-outline-variant/5">
             {topic.category || "Học thuật"}
           </span>
           {isOpen && (
              <span className="px-3 py-1 bg-green-500/5 text-green-600 text-[9px] font-black rounded-lg uppercase tracking-widest border border-green-500/10 flex items-center gap-1.5">
                 <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
                 Mở đăng ký
              </span>
           )}
        </div>
        
        {!isOpen && (
           <span className="px-3 py-1 bg-surface-container-high text-outline/60 text-[9px] font-black rounded-lg uppercase tracking-widest border border-outline-variant/5">
             Đã đóng
           </span>
        )}
      </div>

      <div className="relative z-10 flex-grow">
         <h3 className="text-xl font-black leading-tight text-on-surface mb-4 group-hover:text-primary transition-colors font-headline uppercase tracking-tight">
           {topic.title}
         </h3>
         
         <p className="text-[13px] font-medium text-on-surface-variant mb-8 line-clamp-3 leading-relaxed opacity-70 group-hover:opacity-100 transition-opacity">
           {topic.description}
         </p>
      </div>

      <div className="mt-auto pt-6 border-t border-outline-variant/5 relative z-10">
        <div className="flex items-center gap-4 mb-6 p-4 bg-surface-container-low/30 rounded-[1.5rem] border border-outline-variant/5 transition-colors group-hover:bg-primary/[0.03]">
          <div className="w-11 h-11 rounded-xl bg-primary text-white flex items-center justify-center font-black text-sm shadow-lg shadow-primary/10 border border-white/20 overflow-hidden shrink-0">
            {topic.lecturer?.user?.fullName?.charAt(0) || "L"}
          </div>
          <div>
            <p className="text-[11px] font-black text-on-surface leading-none uppercase tracking-[0.1em]">
              {topic.lecturer?.user?.fullName}
            </p>
            <p className="text-[9px] text-outline font-black mt-1.5 uppercase tracking-widest opacity-60">Giảng viên hướng dẫn</p>
          </div>
        </div>

        <div className="flex justify-between items-center">
          <div className="flex flex-col">
             <div className="flex items-center gap-2 text-outline mb-1">
               <span className="material-symbols-outlined text-[16px]">groups</span>
               <span className="text-[10px] font-black uppercase tracking-widest">
                 {topic.currentGroups || 0} / {topic.maxGroups || 3} nhóm
               </span>
             </div>
             <div className="w-24 h-1 bg-surface-container rounded-full overflow-hidden">
                <div 
                   className="h-full bg-primary transition-all duration-1000" 
                   style={{ width: `${Math.min(100, ((topic.currentGroups || 0) / (topic.maxGroups || 3)) * 100)}%` }}
                ></div>
             </div>
          </div>
          {renderActionButton()}
        </div>
      </div>
    </div>
  );
}
