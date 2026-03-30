import React, { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { registrationApi } from "../api/registrationApi";
import { topicApi } from "../api/topicApi";
import { messageApi } from "../api/messageApi";
import { connectAndSubscribe } from "../ws/stomp";
import { useAuth } from "../context/AuthContext";

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

  const buildName = (u, fallback = "Nguoi dung") => {
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
      setNotice({ type: "danger", message: "Khong tai duoc tin nhan" });
      setTimeout(() => setNotice(null), 2500);
    } finally {
      setChatLoading(false);
    }
  }, []);

  const loadRecipientCandidates = useCallback(async () => {
    const candidates = [];
    const seen = new Set();

    const pushCandidate = (id, name) => {
      if (!id || seen.has(id) || id === user?.id) return;
      seen.add(id);
      candidates.push({ id: String(id), name: name || `User #${id}` });
    };

    try {
      if (isStudent && user?.id) {
        const regs = await registrationApi.getMine(user.id);
        const approved = (regs || []).find((r) => r.approved === true);
        const lecturer = approved?.topic?.lecturer;
        if (lecturer?.id) {
          pushCandidate(lecturer.id, buildName(lecturer?.user || lecturer, "Giang vien"));
        }
      }

      if (isLecturer && topicId) {
        const regs = await registrationApi.getByTopic(topicId);
        (regs || []).forEach((r) => {
          const su = r?.student?.user;
          if (su?.id) pushCandidate(su.id, buildName(su, "Sinh vien"));
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

  // Load inbox messages on component mount (once)
  useEffect(() => {
    loadChatInbox();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Empty dependencies - runs only once

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

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    if (messageContainerRef.current) {
      setTimeout(() => {
        const container = messageContainerRef.current;
        if (container) {
          container.scrollTop = container.scrollHeight + 100;
        }
      }, 150); // Increased delay to ensure DOM fully rendered
    }
  }, [selectedConversation]);

  const handleSendMessage = async () => {
    if (!topicId) {
      setNotice({ type: "warning", message: "Vui long chon de tai truoc khi nhan tin" });
      setTimeout(() => setNotice(null), 2000);
      return;
    }
    if (!selectedRecipientId || !chatText.trim()) {
      setNotice({ type: "warning", message: "Chon nguoi nhan va nhap noi dung" });
      setTimeout(() => setNotice(null), 2000);
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
      setNotice({ type: "success", message: "Da gui tin nhan" });
      setTimeout(() => setNotice(null), 1500);
    } catch (e) {
      console.error(e);
      setNotice({ type: "danger", message: e.message || "Gui tin nhan that bai" });
      setTimeout(() => setNotice(null), 2500);
    } finally {
      setChatSending(false);
    }
  };

  return (
    <div className="container-fluid py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0">
          <i className="bi bi-chat-left-text me-2"></i>
          Kênh trao đổi {isStudent ? "(Sinh viên)" : "(Giảng viên)"}
        </h2>
      </div>

      {notice && (
        <div className={`alert alert-${notice.type} alert-dismissible fade show`} role="alert">
          {notice.message}
          <button type="button" className="btn-close" onClick={() => setNotice(null)}></button>
        </div>
      )}

      {isLecturer && (
        <div className="card mb-4 shadow-sm">
          <div className="card-body">
            <label className="form-label fw-bold">Chọn đề tài</label>
            <select className="form-select" value={topicId} onChange={(e) => setTopicId(e.target.value)}>
              {lecturerTopics.map((t) => (
                <option key={t.id} value={t.id}>{t.title}</option>
              ))}
              {lecturerTopics.length === 0 && <option value="">Không có đề tài nào</option>}
            </select>
          </div>
        </div>
      )}

      <div className="card shadow-sm border-0" id="chat">
        <div className="card-header bg-white d-flex justify-content-between align-items-center">
          <h5 className="mb-0 text-primary">
            <i className="bi bi-chat-dots me-2"></i>
            Trao doi hoc thuat
          </h5>
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={loadChatInbox}>
            <i className="bi bi-arrow-clockwise me-1"></i>Lam moi
          </button>
        </div>
        <div className="card-body">
          <div className="row g-3">
            <div className="col-lg-4">
              <label className="form-label fw-bold">Nguoi nhan</label>
              <select
                className="form-select mb-3"
                value={selectedRecipientId}
                onChange={(e) => setSelectedRecipientId(e.target.value)}
              >
                {recipientCandidates.length === 0 && <option value="">Chua co nguoi trao doi</option>}
                {recipientCandidates.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
              <div className="small text-muted">
                Cuoc tro chuyen duoc gioi han theo nguoi dung dang co lien ket trong de tai/workspace.
              </div>
            </div>

            <div className="col-lg-8">
              <div className="border rounded-3 p-3 bg-light-subtle" style={{ minHeight: 300, maxHeight: 360, overflowY: "auto" }}>
                {chatLoading && <div className="text-muted">Dang tai tin nhan...</div>}
                {!chatLoading && selectedConversation.length === 0 && (
                  <div className="text-muted">Chua co tin nhan cho cuoc tro chuyen nay.</div>
                )}
                {!chatLoading && selectedConversation.map((m) => {
                  const mine = m?.sender?.id === user?.id;
                  const senderName = buildName(m?.sender, "Nguoi gui");
                  return (
                    <div key={m.id} className={`d-flex ${mine ? "justify-content-end" : "justify-content-start"} mb-2`}>
                      <div className={`p-2 rounded-3 ${mine ? "bg-primary text-white" : "bg-white border"}`} style={{ maxWidth: "82%" }}>
                        <div className={`small ${mine ? "text-white-50" : "text-muted"}`}>{senderName}</div>
                        <div>{m.content}</div>
                        <div className={`small mt-1 ${mine ? "text-white-50" : "text-muted"}`}>
                          {m.createdAt ? new Date(m.createdAt).toLocaleString() : ""}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="d-flex gap-2 mt-3">
                <input
                  className="form-control"
                  placeholder="Nhap noi dung trao doi hoc thuat..."
                  value={chatText}
                  onChange={(e) => setChatText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      if (!chatSending && selectedRecipientId && topicId) {
                        handleSendMessage();
                      }
                    }
                  }}
                />
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleSendMessage}
                  disabled={chatSending || !selectedRecipientId || !topicId}
                >
                  {chatSending ? "Dang gui..." : "Gui"}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
