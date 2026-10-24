import { useCallback, useState } from "react";
import { discussApi } from "../api/discussApi";
import InlineNotice from "../components/InlineNotice";
import TopicPicker from "../components/TopicPicker";
import { EmptyState, ErrorState, LoadingState } from "../components/PageState";
import useLoadEffect from "../hooks/useLoadEffect";
import useMyTopics from "../hooks/useMyTopics";
import ThreadList from "./discussion/ThreadList";
import ThreadView from "./discussion/ThreadView";

const PAGE_SIZE = 50;

/** Ghép trang bài viết mới vào danh sách, bỏ bài trùng (bài vừa gửi có thể nằm ở trang tải sau). */
const mergePosts = (prev, next) => {
  const seen = new Set(prev.map((p) => p.id));
  return [...prev, ...next.filter((p) => !seen.has(p.id))];
};

/** P4-11 — Kênh thảo luận nhóm theo đề tài: chủ đề + bài viết, dành cho thành viên đề tài. */
export default function DiscussionPage() {
  const { topics, topicId, setTopicId, loading: topicsLoading, error: topicsError, isLecturer, userId } = useMyTopics();

  const [threads, setThreads] = useState([]);
  const [threadsLoading, setThreadsLoading] = useState(false);
  const [error, setError] = useState("");
  const [picked, setPicked] = useState(null);
  const [posts, setPosts] = useState([]);
  const [page, setPage] = useState({ number: 0, totalPages: 0 });
  const [postsLoading, setPostsLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [sending, setSending] = useState(false);
  const [notice, setNotice] = useState(null);

  const loadThreads = useCallback(async () => {
    if (!topicId) {
      setThreads([]);
      return;
    }
    setThreadsLoading(true);
    setError("");
    try {
      setThreads((await discussApi.threads(topicId)) || []);
    } catch (e) {
      setError(e.message || "Không tải được các chủ đề.");
    } finally {
      setThreadsLoading(false);
    }
  }, [topicId]);
  useLoadEffect(loadThreads);

  const thread = threads.find((t) => t.id === picked) || threads[0] || null;
  const threadId = thread?.id ?? null;

  const loadPosts = useCallback(async () => {
    if (!threadId) {
      setPosts([]);
      return;
    }
    setPostsLoading(true);
    try {
      const data = await discussApi.posts(threadId, { page: 0, size: PAGE_SIZE });
      setPosts(data.items || []);
      setPage({ number: 0, totalPages: data.totalPages || 0 });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Không tải được bài viết." });
    } finally {
      setPostsLoading(false);
    }
  }, [threadId]);
  useLoadEffect(loadPosts);

  const loadMore = async () => {
    setPostsLoading(true);
    try {
      const next = page.number + 1;
      const data = await discussApi.posts(threadId, { page: next, size: PAGE_SIZE });
      setPosts((prev) => mergePosts(prev, data.items || []));
      setPage({ number: next, totalPages: data.totalPages || 0 });
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Không tải được bài viết." });
    } finally {
      setPostsLoading(false);
    }
  };

  const createThread = async (title) => {
    setCreating(true);
    try {
      const created = await discussApi.createThread({ topicId: Number(topicId), title });
      setThreads((list) => [created, ...list]);
      setPicked(created.id);
      return true;
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Không tạo được chủ đề." });
      return false;
    } finally {
      setCreating(false);
    }
  };

  const send = async (content) => {
    setSending(true);
    try {
      const post = await discussApi.addPost(threadId, { content });
      setPosts((prev) => mergePosts(prev, [post]));
      return true;
    } catch (e) {
      setNotice({ type: "danger", message: e.message || "Không gửi được bài viết." });
      return false;
    } finally {
      setSending(false);
    }
  };

  let body;
  if (topicsLoading) body = <LoadingState />;
  else if (topicsError) body = <ErrorState message={topicsError} />;
  else if (topics.length === 0)
    body = (
      <EmptyState icon="forum" title="Chưa có kênh thảo luận">
        {isLecturer ? "Kênh thảo luận được tạo theo từng đề tài bạn hướng dẫn."
          : "Bạn tham gia kênh thảo luận của đề tài sau khi đăng ký được duyệt."}
      </EmptyState>
    );
  else if (error) body = <ErrorState message={error} onRetry={loadThreads} />;
  else if (threadsLoading && threads.length === 0) body = <LoadingState label="Đang tải chủ đề..." />;
  else
    body = (
      <div className="grid grid-cols-1 lg:grid-cols-[20rem_1fr] gap-6">
        <aside className="bg-surface-container-low rounded-[2rem] p-4" aria-label="Chủ đề">
          <ThreadList threads={threads} selectedId={threadId} onSelect={setPicked} onCreate={createThread} creating={creating} />
        </aside>
        <section className="bg-white dark:bg-slate-900 rounded-[2rem] border border-outline-variant/10 shadow-sm overflow-hidden">
          {thread ? (
            <ThreadView key={thread.id} thread={thread} posts={posts} myUserId={userId}
              hasMore={page.number + 1 < page.totalPages} loadingMore={postsLoading} onLoadMore={loadMore}
              onSend={send} sending={sending} />
          ) : (
            <div className="p-10 text-center text-sm text-outline">Chọn hoặc mở một chủ đề để bắt đầu thảo luận.</div>
          )}
        </section>
      </div>
    );

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
        <div>
          <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary mb-1 block">Làm việc nhóm</span>
          <h1 className="text-3xl md:text-4xl font-black font-headline tracking-tighter uppercase">Kênh thảo luận</h1>
          <p className="text-sm text-on-surface-variant mt-2">
            Thảo luận theo chủ đề giữa giảng viên và các sinh viên cùng đề tài. Trao đổi riêng: dùng mục Trao đổi học thuật.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <TopicPicker topics={topics} value={topicId} onChange={(v) => { setTopicId(v); setPicked(null); }} />
          {topics.length > 0 && (
            <button type="button" onClick={() => { loadThreads(); loadPosts(); }} aria-label="Làm mới"
              className="rounded-xl p-2.5 border border-outline-variant/20 hover:bg-surface-container">
              <span className="material-symbols-outlined" aria-hidden="true">refresh</span>
            </button>
          )}
        </div>
      </div>

      {notice && <InlineNotice type={notice.type} message={notice.message} onClose={() => setNotice(null)} autoHideMs={4000} />}

      {body}
    </div>
  );
}
