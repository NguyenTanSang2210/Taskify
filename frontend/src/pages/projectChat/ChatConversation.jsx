/** Tách từ ProjectChatPage.jsx (P3-11). */
export default function ChatConversation({ buildName, chatLoading, chatSending, chatText, convInfo, handleSendMessage, loadOlderMessages, loadingOlder, messageContainerRef, selectedConversation, selectedPeer, setChatText, user }) {
  return (
    <section className="flex-1 flex flex-col min-w-0">
       {selectedPeer ? (
          <>
             {/* Chat Header */}
             <div className="px-8 py-5 border-b border-outline-variant/5 flex items-center justify-between bg-white dark:bg-slate-900 z-10">
                <div className="flex items-center gap-4">
                   <div className="w-10 h-10 rounded-xl bg-secondary/10 text-secondary flex items-center justify-center font-black">
                      {selectedPeer.avatar}
                   </div>
                   <div>
                      <h3 className="text-sm font-black text-on-surface uppercase tracking-tight">{selectedPeer.name}</h3>
                   </div>
                </div>
             </div>
    
             {/* Messages List */}
             <div 
                ref={messageContainerRef}
                className="flex-grow overflow-y-auto p-8 space-y-6 bg-surface-container-lowest/30"
             >
                {chatLoading ? (
                   <div className="flex flex-col items-center justify-center h-full text-outline [&_.material-symbols-outlined]:opacity-40">
                      <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent mb-4"></div>
                      <p className="text-[10px] font-black uppercase tracking-widest text-outline">Đang tải cuộc hội thoại...</p>
                   </div>
                ) : selectedConversation.length === 0 ? (
                   <div className="flex flex-col items-center justify-center h-full text-outline [&_.material-symbols-outlined]:opacity-40 text-center px-4">
                      <span className="material-symbols-outlined text-6xl mb-4">forum</span>
                      <h3 className="text-sm font-black uppercase tracking-[0.2em] text-outline">Bắt đầu trò chuyện</h3>
                      <p className="text-[10px] font-bold text-outline mt-2 leading-relaxed uppercase">Hãy gửi lời chào đầu tiên để bắt đầu thảo luận về đề tài.</p>
                   </div>
                ) : (
                   <>
                   {convInfo.page < convInfo.totalPages - 1 && (
                      <div className="flex justify-center">
                         <button
                           type="button"
                           onClick={loadOlderMessages}
                           disabled={loadingOlder}
                           className="px-5 py-2 bg-surface-container text-on-surface-variant rounded-xl font-black text-[9px] uppercase tracking-widest hover:bg-surface-container-high transition-all disabled:opacity-50"
                         >
                           {loadingOlder ? "Đang tải…" : "Tải tin cũ hơn"}
                         </button>
                      </div>
                   )}
                   {selectedConversation.map((m, idx) => {
                      const mine = m?.sender?.id === user?.id;
                      const showSender = idx === 0 || selectedConversation[idx-1]?.sender?.id !== m?.sender?.id;
                      return (
                         <div key={m.id} className={`flex flex-col ${mine ? "items-end" : "items-start"} space-y-1`}>
                            {showSender && !mine && (
                               <span className="text-[9px] font-black text-outline uppercase tracking-widest ml-1">{buildName(m?.sender)}</span>
                            )}
                            <div className={`px-5 py-3.5 rounded-2xl max-w-[70%] group relative shadow-sm ${
                               mine 
                               ? "bg-primary text-on-primary rounded-tr-none" 
                               : "bg-white dark:bg-slate-800 text-on-surface-variant border border-outline-variant/10 rounded-tl-none"
                            }`}>
                               <p className="text-sm font-medium leading-relaxed">{m.content}</p>
                               <div className={`text-[8px] font-bold mt-1.5 uppercase tracking-tighter ${mine ? "text-on-primary/80" : "text-outline"}`}>
                                  {new Date(m.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                               </div>
                            </div>
                         </div>
                      );
                   })}
                   </>
                )}
             </div>
    
             {/* Input Area */}
             <div className="px-8 py-6 bg-white dark:bg-slate-900 border-t border-outline-variant/5 shrink-0">
                <div className="flex items-center gap-4 bg-surface-container-low/50 p-2 rounded-[1.5rem] border border-outline-variant/10 focus-within:ring-2 ring-primary/10 transition-all">
                   <input aria-label="Nội dung tin nhắn"
                      className="flex-grow bg-transparent border-none focus:ring-0 text-sm font-medium placeholder:text-outline/50 py-2"
                      placeholder="Viết tin nhắn cho công việc học thuật..."
                      value={chatText}
                      onChange={(e) => setChatText(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" && !e.shiftKey) {
                          e.preventDefault();
                          handleSendMessage();
                        }
                      }}
                   />
                   <button aria-label="Gửi tin nhắn" 
                      onClick={handleSendMessage}
                      disabled={chatSending || !chatText.trim()}
                      className={`w-11 h-11 rounded-full flex items-center justify-center transition-all shrink-0 ${
                        chatText.trim() ? "bg-primary text-on-primary shadow-lg shadow-primary/20 scale-100" : "bg-outline/10 text-outline scale-90"
                      } active:scale-95`}
                   >
                      <span className="material-symbols-outlined">{chatSending ? 'sync' : 'send'}</span>
                   </button>
                </div>
             </div>
          </>
       ) : (
          <div className="flex-grow flex flex-col items-center justify-center p-20 text-center text-outline [&_.material-symbols-outlined]:opacity-40 select-none">
             <div className="w-32 h-32 bg-surface-container rounded-full flex items-center justify-center mb-8">
                <span className="material-symbols-outlined text-6xl">chat_bubble</span>
             </div>
             <h2 className="text-xl font-black uppercase tracking-[0.3em] text-outline">Kênh đàm thoại</h2>
             <p className="text-[10px] font-bold text-outline mt-3 uppercase tracking-widest max-w-[280px]">Vui lòng chọn một người trong danh sách để bắt đầu trao đổi chi tiết về đề tài nghiên cứu.</p>
          </div>
       )}
    </section>
  );
}
