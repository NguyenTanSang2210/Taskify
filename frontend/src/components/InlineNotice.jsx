export default function InlineNotice({ type = "success", message, onClose, autoHideMs }) {
  if (!message) return null;
  if (autoHideMs && onClose) {
    setTimeout(onClose, autoHideMs);
  }
  const className =
    type === "danger"
      ? "inline-notice inline-notice-danger"
      : type === "warning"
      ? "inline-notice inline-notice-warning"
      : "inline-notice inline-notice-success";

  const renderIcon = () => {
    if (type === "success") {
      return (
        <svg className="checkmark" viewBox="0 0 52 52" aria-hidden="true">
          <circle className="checkmark__circle" cx="26" cy="26" r="25" fill="none" />
          <path className="checkmark__check" fill="none" d="M14 27l7 7 16-16" />
        </svg>
      );
    }
    if (type === "danger") {
      return (
        <svg className="crossmark" viewBox="0 0 52 52" aria-hidden="true">
          <circle className="crossmark__circle" cx="26" cy="26" r="25" fill="none" />
          <path className="crossmark__cross" fill="none" d="M18 18l16 16M34 18L18 34" />
        </svg>
      );
    }
    return (
      <svg className="warnmark" viewBox="0 0 52 52" aria-hidden="true">
        <circle className="warnmark__circle" cx="26" cy="26" r="25" fill="none" />
        <path className="warnmark__excl" fill="none" d="M26 14v18M26 38v2" />
      </svg>
    );
  };

  return (
    <div className={className} role="alert">
      {renderIcon()}
      <span className="ms-2">{message}</span>
      {onClose && (
        <button type="button" className="btn btn-sm btn-link ms-3" onClick={onClose}>
          Đóng
        </button>
      )}
    </div>
  );
}
