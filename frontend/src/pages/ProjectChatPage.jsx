import React, { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { registrationApi } from "../api/registrationApi";
import { topicApi } from "../api/topicApi";
import { messageApi } from "../api/messageApi";
import { connectAndSubscribe } from "../ws/stomp";
import { useAuth } from "../context/AuthContext";
import InlineNotice from "../components/InlineNotice";

export default function ProjectChatPage() {
  const { user } = useAuth();
  const role = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isStudent = role === "STUDENT";
  const isLecturer = role === "LECTURER";

  const [topicId, setTopicId] = useState("");
  const [lecturerTopics, setLecturerTopics] = useState([]);
  const [notice, setNotice] = useState(null);

  const [inboxMessages, setInboxMessages] = useState([]);
  const [recipientCandidates, setRecipientCandidates] = useState([]);
  const [selectedRecipientId, setSelectedRecipientId] = useState("");
  const [chatText, setChatText] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const [chatSending, setChatSending] = useState(false);
  
  const messageContainerRef = useRef(null);

  const upsertMessage = useCallback((message) => {
    if (!message?.id) return;
    setInboxMessages((prev) => {
      const exists = prev.some((x) => x.id === message.id);
      if (exists) {
        return prev.map((x) => (x.id === message.id ? message : x));
      }
      return [message, ...prev];
    });
  }, []);

  const buildName = (u, fallback = "Người dùng") => {
    if (!u) return fallback;
    return u.fullName || u.name || u.username || fallback;
  };

  const loadStudentTopic = useCallback(async () => {
    if (!isStudent || !user?.id) return;
    try {
      const data = await registrationApi.getMine(user.id);
      const approved = (data || []).filter((r) => r.approved === true);
      if (approved.length > 0) {
        setTopicId(String(approved[0].topic?.id || ""));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isStudent, user?.id]);

  const loadLecturerTopics = useCallback(async () => {
    if (!isLecturer || !user?.id) return;
    try {
      const data = await topicApi.getAllTopics({ lecturerId: user.id, size: 100 });
      setLecturerTopics(data.items || []);
      if (data.items?.length > 0 && !topicId) {
        setTopicId(String(data.items[0].id));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isLecturer, user?.id, topicId]);

  const loadChatInbox = useCallback(async () => {
    setChatLoading(true);
    try {
      const data = await messageApi.inbox();
      setInboxMessages(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không tải được tin nhắn" });
    } finally {
      setChatLoading(false);
    }
  }, []);

  const loadRecipientCandidates = useCallback(async () => {
    const candidates = [];
    const seen = new Set();

    const pushCandidate = (id, name, avatarData) => {
      if (!id || seen.has(id) || id === user?.id) return;
      seen.add(id);
      candidates.push({ 
        id: String(id), 
        name: name || `User #${id}`,
        avatar: avatarData || name?.charAt(0) || "?"
      });
    };

    try {
      if (isStudent && user?.id) {
        const regs = await registrationApi.getMine(user.id);
        const approved = (regs || []).find((r) => r.approved === true);
        const topic = approved?.topic;
        if (topic?.lecturerId) {
          pushCandidate(topic.lecturerId, topic.lecturerName || "Giảng viên");
        }
      }

      if (isLecturer && topicId) {
        const regs = await registrationApi.getByTopic(topicId);
        (regs || []).forEach((r) => {
          if (r?.studentId) pushCandidate(r.studentId, r.studentName || "Sinh viên");
        });
      }
    } catch (e) {
      console.error(e);
    }

    setRecipientCandidates(candidates);
  }, [isStudent, isLecturer, topicId, user?.id]);

  useEffect(() => {
    if (isStudent) loadStudentTopic();
    if (isLecturer) loadLecturerTopics();
  }, [loadStudentTopic, loadLecturerTopics, isStudent, isLecturer]);

  useEffect(() => {
    loadChatInbox();
  }, [loadChatInbox]);

  useEffect(() => {
    if (!user?.id) return;
    const client = connectAndSubscribe({
      destination: `/topic/messages/${user.id}`,
      onMessage: (payload) => {
        if (payload?.type !== "MESSAGE_CREATED") return;
        const msg = payload?.message;
        if (!msg) return;
        upsertMessage(msg);
      },
    });
    return () => client?.deactivate?.();
  }, [user?.id, upsertMessage]);

  useEffect(() => {
    loadRecipientCandidates();
  }, [loadRecipientCandidates]);

  useEffect(() => {
    if (selectedRecipientId) return;
    if (recipientCandidates.length === 0) return;
    setSelectedRecipientId(recipientCandidates[0].id);
  }, [recipientCandidates, selectedRecipientId]);

  const selectedConversation = useMemo(() => {
    if (!selectedRecipientId) return [];
    const rid = Number(selectedRecipientId);
    return inboxMessages
      .filter((m) => {
        const senderId = m?.sender?.id;
        const recipientId = m?.recipient?.id;
        const isPeerMsg =
          (senderId === user?.id && recipientId === rid) ||
          (senderId === rid && recipientId === user?.id);
        if (!isPeerMsg) return false;
        if (!topicId) return true;
        return !m?.topic?.id || String(m.topic.id) === String(topicId);
      })
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  }, [inboxMessages, selectedRecipientId, user?.id, topicId]);

  useEffect(() => {
    if (messageContainerRef.current) {
      setTimeout(() => {
        const container = messageContainerRef.current;
        if (container) {
          container.scrollTop = container.scrollHeight;
        }
      }, 100);
    }
  }, [selectedConversation]);

  const handleSendMessage = async () => {
    if (!topicId) {
      setNotice({ type: "warning", message: "Vui lòng chọn đề tài trước khi nhắn tin." });
      return;
    }
    if (!selectedRecipientId || !chatText.trim()) {
      return;
    }

    setChatSending(true);
    try {
      const sent = await messageApi.send({
        recipientId: selectedRecipientId,
        content: chatText.trim(),
        topicId: String(topicId),
      });
      setChatText("");
      upsertMessage(sent);
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Gửi tin nhắn thất bại." });
    } finally {
      setChatSending(false);
    }
  };

  const selectedPeer = recipientCandidates.find(c => c.id === selectedRecipientId);

  return (
    <div className="h-[calc(100vh-120px)] flex flex-col space-y-6 animate-in fade-in duration-500">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 shrink-0">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-1 block">Communication Hub</span>
          <h1 className="text-3xl font-black text-on-surface tracking-tight font-headline uppercase leading-none">Trao đổi học thuật</h1>
          <p className="text-xs text-outline mt-2 font-bold uppercase tracking-tight">Kênh liên lạc trực tiếp giữa Giảng viên và Sinh viên theo đề tài.</p>
        </div>
        
        {isLecturer && (
           <div className="flex items-center gap-3 bg-white dark:bg-slate-900 px-4 py-2 rounded-2xl border border-outline-variant/10 shadow-sm">
              <span className="text-[10px] font-black text-outline uppercase tracking-widest shrink-0">Đề tài:</span>
              <select 
                className="bg-transparent border-none focus:ring-0 text-xs font-black uppercase tracking-widest text-primary cursor-pointer outline-none max-w-[200px]" 
                value={topicId} 
                onChange={(e) => setTopicId(e.target.value)}
              >
                {lecturerTopics.map((t) => (
                  <option key={t.id} value={t.id}>{t.title}</option>
                ))}
                {lecturerTopics.length === 0 && <option value="">Chưa có đề tài nào</option>}
              </select>
           </div>
        )}
      </div>

      {notice && (
        <div className="max-w-2xl mx-auto shrink-0 w-full px-4">
          <InlineNotice
            type={notice.type}
            message={notice.message}
            onClose={() => setNotice(null)}
            autoHideMs={3000}
          />
        </div>
      )}

      {/* Chat Container */}
      <div className="flex-grow flex bg-white dark:bg-slate-900 rounded-[2.5rem] border border-outline-variant/10 shadow-sm overflow-hidden min-h-0">
        
        {/* Sidebar: People */}
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

        {/* Main Chat Area */}
        <main className="flex-1 flex flex-col min-w-0">
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
                          <div className="flex items-center gap-1.5 mt-0.5">
                             <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
                             <span className="text-[9px] font-black text-green-500 uppercase tracking-widest">Đang hoạt động</span>
                          </div>
                       </div>
                    </div>
                    <div className="flex items-center gap-2">
                       <button className="p-2.5 rounded-xl hover:bg-surface-container text-outline transition-colors" title="Cuộc gọi video">
                          <span className="material-symbols-outlined">videocam</span>
                       </button>
                       <button className="p-2.5 rounded-xl hover:bg-surface-container text-outline transition-colors" title="Thông tin chi tiết">
                          <span className="material-symbols-outlined">info</span>
                       </button>
                    </div>
                 </div>

                 {/* Messages List */}
                 <div 
                    ref={messageContainerRef}
                    className="flex-grow overflow-y-auto p-8 space-y-6 bg-surface-container-lowest/30"
                 >
                    {chatLoading ? (
                       <div className="flex flex-col items-center justify-center h-full opacity-30">
                          <div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent mb-4"></div>
                          <p className="text-[10px] font-black uppercase tracking-widest text-outline">Đang tải cuộc hội thoại...</p>
                       </div>
                    ) : selectedConversation.length === 0 ? (
                       <div className="flex flex-col items-center justify-center h-full opacity-30 text-center px-4">
                          <span className="material-symbols-outlined text-6xl mb-4">forum</span>
                          <h3 className="text-sm font-black uppercase tracking-[0.2em] text-outline">Bắt đầu trò chuyện</h3>
                          <p className="text-[10px] font-bold text-outline mt-2 leading-relaxed uppercase">Hãy gửi lời chào đầu tiên để bắt đầu thảo luận về đề tài.</p>
                       </div>
                    ) : (
                       selectedConversation.map((m, idx) => {
                          const mine = m?.sender?.id === user?.id;
                          const showSender = idx === 0 || selectedConversation[idx-1]?.sender?.id !== m?.sender?.id;
                          return (
                             <div key={m.id} className={`flex flex-col ${mine ? "items-end" : "items-start"} space-y-1`}>
                                {showSender && !mine && (
                                   <span className="text-[9px] font-black text-outline uppercase tracking-widest ml-1">{buildName(m?.sender)}</span>
                                )}
                                <div className={`px-5 py-3.5 rounded-2xl max-w-[70%] group relative shadow-sm ${
                                   mine 
                                   ? "bg-primary text-white rounded-tr-none" 
                                   : "bg-white dark:bg-slate-800 text-on-surface-variant border border-outline-variant/10 rounded-tl-none"
                                }`}>
                                   <p className="text-sm font-medium leading-relaxed">{m.content}</p>
                                   <div className={`text-[8px] font-bold mt-1.5 uppercase tracking-tighter ${mine ? "text-white/60" : "text-outline/50"}`}>
                                      {new Date(m.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                                   </div>
                                </div>
                             </div>
                          );
                       })
                    )}
                 </div>

                 {/* Input Area */}
                 <div className="px-8 py-6 bg-white dark:bg-slate-900 border-t border-outline-variant/5 shrink-0">
                    <div className="flex items-center gap-4 bg-surface-container-low/50 p-2 rounded-[1.5rem] border border-outline-variant/10 focus-within:ring-2 ring-primary/10 transition-all">
                       <button className="p-3 text-outline hover:text-primary transition-colors shrink-0">
                          <span className="material-symbols-outlined">add_circle</span>
                       </button>
                       <input 
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
                       <button 
                          onClick={handleSendMessage}
                          disabled={chatSending || !chatText.trim()}
                          className={`w-11 h-11 rounded-full flex items-center justify-center transition-all shrink-0 ${
                            chatText.trim() ? "bg-primary text-white shadow-lg shadow-primary/20 scale-100" : "bg-outline/10 text-outline scale-90"
                          } active:scale-95`}
                       >
                          <span className="material-symbols-outlined">{chatSending ? 'sync' : 'send'}</span>
                       </button>
                    </div>
                 </div>
              </>
           ) : (
              <div className="flex-grow flex flex-col items-center justify-center p-20 text-center opacity-30 select-none">
                 <div className="w-32 h-32 bg-surface-container rounded-full flex items-center justify-center mb-8">
                    <span className="material-symbols-outlined text-6xl">chat_bubble</span>
                 </div>
                 <h2 className="text-xl font-black uppercase tracking-[0.3em] text-outline">Kênh đàm thoại</h2>
                 <p className="text-[10px] font-bold text-outline mt-3 uppercase tracking-widest max-w-[280px]">Vui lòng chọn một người trong danh sách để bắt đầu trao đổi chi tiết về đề tài nghiên cứu.</p>
              </div>
           )}
        </main>
      </div>
    </div>
  );
}
