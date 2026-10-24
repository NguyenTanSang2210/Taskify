import { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { registrationApi } from "../api/registrationApi";
import { topicApi } from "../api/topicApi";
import { messageApi } from "../api/messageApi";
import { connectAndSubscribe } from "../ws/stomp";
import { useAuth } from "../context/AuthContext";
import InlineNotice from "../components/InlineNotice";
import ChatPeopleSidebar from "./projectChat/ChatPeopleSidebar";
import ChatConversation from "./projectChat/ChatConversation";
import useLoadEffect from '../hooks/useLoadEffect';

export default function ProjectChatPage() {
  const { user } = useAuth();
  const userId = user?.id;
  const role = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;
  const isStudent = role === "STUDENT";
  const isLecturer = role === "LECTURER";

  const [topicId, setTopicId] = useState("");
  const [lecturerTopics, setLecturerTopics] = useState([]);
  const [notice, setNotice] = useState(null);

  const [inboxMessages, setInboxMessages] = useState([]);
  const [recipientCandidates, setRecipientCandidates] = useState([]);
  const [pickedRecipientId, setSelectedRecipientId] = useState("");
  // Chưa chọn ai thì mặc định là người đầu tiên trong danh sách (suy ra, không đồng bộ bằng effect)
  const selectedRecipientId = pickedRecipientId || recipientCandidates[0]?.id || "";
  const [chatText, setChatText] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const [convInfo, setConvInfo] = useState({ page: 0, totalPages: 0 });
  const [loadingOlder, setLoadingOlder] = useState(false);
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
    if (!isStudent || !userId) return;
    try {
      const data = await registrationApi.getMine(userId);
      const approved = (data || []).filter((r) => r.approved === true);
      if (approved.length > 0) {
        setTopicId(String(approved[0].topic?.id || ""));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isStudent, userId]);

  const loadLecturerTopics = useCallback(async () => {
    if (!isLecturer || !userId) return;
    try {
      const data = await topicApi.getAllTopics({ lecturerId: userId, size: 100 });
      setLecturerTopics(data.items || []);
      if (data.items?.length > 0 && !topicId) {
        setTopicId(String(data.items[0].id));
      }
    } catch (e) {
      console.error(e);
    }
  }, [isLecturer, userId, topicId]);

  // Hội thoại với người đang chọn trong đề tài đang chọn: trang mới nhất trước (P3-06)
  const loadChatInbox = useCallback(async () => {
    if (!selectedRecipientId) {
      setInboxMessages([]);
      setConvInfo({ page: 0, totalPages: 0 });
      return;
    }
    setChatLoading(true);
    try {
      const data = await messageApi.conversation(selectedRecipientId, { topicId });
      setInboxMessages(data?.items || []);
      setConvInfo({ page: 0, totalPages: data?.totalPages || 0 });
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: "Không tải được tin nhắn" });
    } finally {
      setChatLoading(false);
    }
  }, [selectedRecipientId, topicId]);

  const loadOlderMessages = async () => {
    const next = convInfo.page + 1;
    setLoadingOlder(true);
    try {
      const data = await messageApi.conversation(selectedRecipientId, { topicId, page: next });
      setInboxMessages((prev) => {
        const seen = new Set(prev.map((x) => x.id));
        return [...prev, ...(data?.items || []).filter((x) => !seen.has(x.id))];
      });
      setConvInfo({ page: next, totalPages: data?.totalPages || 0 });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Không tải được tin nhắn cũ hơn" });
    } finally {
      setLoadingOlder(false);
    }
  };

  const loadRecipientCandidates = useCallback(async () => {
    const candidates = [];
    const seen = new Set();

    const pushCandidate = (id, name, avatarData) => {
      if (!id || seen.has(id) || id === userId) return;
      seen.add(id);
      candidates.push({ 
        id: String(id), 
        name: name || `User #${id}`,
        avatar: avatarData || name?.charAt(0) || "?"
      });
    };

    try {
      if (isStudent && userId) {
        const regs = await registrationApi.getMine(userId);
        const approved = (regs || []).find((r) => r.approved === true);
        const topic = approved?.topic;
        if (topic?.lecturerId) {
          pushCandidate(topic.lecturerId, topic.lecturerName || "Giảng viên");
        }
      }

      if (isLecturer && topicId) {
        const regs = await registrationApi.getByTopic(topicId);
        // Chỉ sinh viên ĐÃ ĐƯỢC DUYỆT mới là thành viên đề tài; backend từ chối tin nhắn tới người ngoài đề tài
        (regs || []).forEach((r) => {
          if (r?.approved === true && r?.studentId) pushCandidate(r.studentId, r.studentName || "Sinh viên");
        });
      }
    } catch (e) {
      console.error(e);
    }

    setRecipientCandidates(candidates);
  }, [isStudent, isLecturer, topicId, userId]);

  const loadTopics = useCallback(() => {
    if (isStudent) loadStudentTopic();
    if (isLecturer) loadLecturerTopics();
  }, [loadStudentTopic, loadLecturerTopics, isStudent, isLecturer]);

  useLoadEffect(loadTopics);

  useLoadEffect(loadChatInbox);

  useEffect(() => {
    if (!userId) return;
    const client = connectAndSubscribe({
      destination: "/user/queue/messages",
      onMessage: (payload) => {
        if (payload?.type !== "MESSAGE_CREATED") return;
        const msg = payload?.message;
        if (!msg) return;
        upsertMessage(msg);
      },
    });
    return () => client?.deactivate?.();
  }, [userId, upsertMessage]);

  useLoadEffect(loadRecipientCandidates);

  const selectedConversation = useMemo(() => {
    if (!selectedRecipientId) return [];
    const rid = Number(selectedRecipientId);
    return inboxMessages
      .filter((m) => {
        const senderId = m?.sender?.id;
        const recipientId = m?.recipient?.id;
        const isPeerMsg =
          (senderId === userId && recipientId === rid) ||
          (senderId === rid && recipientId === userId);
        if (!isPeerMsg) return false;
        if (!topicId) return true;
        return String(m?.topic?.id ?? "") === String(topicId);
      })
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  }, [inboxMessages, selectedRecipientId, userId, topicId]);

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
              <label htmlFor="chat-topic" className="text-[10px] font-black text-outline uppercase tracking-widest shrink-0">Đề tài:</label>
              <select id="chat-topic"
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
        <ChatPeopleSidebar loadChatInbox={loadChatInbox} recipientCandidates={recipientCandidates} selectedRecipientId={selectedRecipientId} setSelectedRecipientId={setSelectedRecipientId} />

        {/* Main Chat Area */}
        <ChatConversation buildName={buildName} chatLoading={chatLoading} chatSending={chatSending} chatText={chatText} convInfo={convInfo} handleSendMessage={handleSendMessage} loadOlderMessages={loadOlderMessages} loadingOlder={loadingOlder} messageContainerRef={messageContainerRef} selectedConversation={selectedConversation} selectedPeer={selectedPeer} setChatText={setChatText} user={user} />
      </div>
    </div>
  );
}
