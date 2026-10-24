import { useCallback, useState } from "react";
import { registrationApi } from "../api/registrationApi";
import { topicApi } from "../api/topicApi";
import { useAuth } from "../context/AuthContext";
import useLoadEffect from "./useLoadEffect";

/**
 * Các đề tài người dùng là thành viên: SV — đề tài đã được duyệt; GV — đề tài mình hướng dẫn.
 * `topicId` mặc định là đề tài đầu tiên cho tới khi người dùng chọn đề tài khác.
 */
export default function useMyTopics() {
  const { user } = useAuth();
  const userId = user?.id;
  const role = typeof user?.role === "object" && user?.role ? user.role.name : user?.role;

  const [topics, setTopics] = useState([]);
  const [picked, setPicked] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (!userId || (role !== "STUDENT" && role !== "LECTURER")) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    try {
      if (role === "STUDENT") {
        const regs = await registrationApi.getMine(userId);
        const approved = (regs || []).filter((r) => r.approved === true && r.topic?.id);
        setTopics(approved.map((r) => ({ id: r.topic.id, title: r.topic.title })));
      } else {
        const data = await topicApi.getAllTopics({ lecturerId: userId, size: 100 });
        setTopics((data.items || []).map((t) => ({ id: t.id, title: t.title })));
      }
    } catch (e) {
      setError(e.message || "Không tải được danh sách đề tài.");
    } finally {
      setLoading(false);
    }
  }, [userId, role]);
  useLoadEffect(load);

  const topicId = topics.some((t) => String(t.id) === picked) ? picked : topics[0] ? String(topics[0].id) : "";

  return { topics, topicId, setTopicId: setPicked, loading, error, role, isLecturer: role === "LECTURER", userId };
}
