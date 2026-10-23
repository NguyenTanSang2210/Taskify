/** Tách từ ProjectChatPage.jsx (P3-11). */
export default function ChatPeopleSidebar({ loadChatInbox, recipientCandidates, selectedRecipientId, setSelectedRecipientId }) {
  return (
    <aside className="w-80 border-r border-outline-variant/5 flex flex-col shrink-0 bg-surface-container-low/20">
       <div className="p-6 border-b border-outline-variant/5">
          <h2 className="text-[10px] font-black uppercase tracking-[0.2em] text-outline">Danh bạ đề tài</h2>
       </div>
       
       <div className="flex-grow overflow-y-auto p-4 space-y-2">
          {recipientCandidates.length === 0 ? (
            <div className="py-10 text-center opacity-30">
               <span className="material-symbols-outlined text-4xl mb-2">person_off</span>
               <p className="text-[10px] font-black uppercase tracking-widest">Không có người nhận</p>
            </div>
          ) : (
            recipientCandidates.map((c) => (
               <button 
                  key={c.id} 
                  onClick={() => setSelectedRecipientId(c.id)}
                  className={`w-full flex items-center gap-4 p-4 rounded-2xl transition-all duration-300 ${
                    selectedRecipientId === c.id 
                    ? "bg-primary text-white shadow-lg shadow-primary/20" 
                    : "hover:bg-surface-container text-on-surface-variant"
                  }`}
               >
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center font-black uppercase shrink-0 ${
                    selectedRecipientId === c.id ? "bg-white/20" : "bg-primary/10 text-primary"
                  }`}>
                     {c.avatar}
                  </div>
                  <div className="text-left overflow-hidden">
                     <p className="text-[11px] font-black uppercase tracking-widest truncate">{c.name}</p>
                     <p className={`text-[9px] font-bold uppercase tracking-tight mt-0.5 truncate ${
                        selectedRecipientId === c.id ? "text-white/60" : "text-outline"
                     }`}>Trực tuyến</p>
                  </div>
               </button>
            ))
          )}
       </div>
    
       <div className="p-6 bg-surface-container-low/50 border-t border-outline-variant/5">
          <button 
            onClick={loadChatInbox}
            className="w-full py-3 bg-white dark:bg-slate-800 border border-outline-variant/10 rounded-xl text-[10px] font-black uppercase tracking-widest text-outline hover:text-primary transition-all flex items-center justify-center gap-2"
          >
             <span className="material-symbols-outlined text-sm">sync</span>
             Cập nhật tin nhắn
          </button>
       </div>
    </aside>
  );
}
