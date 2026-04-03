import React, { useEffect } from "react";

export default function InlineNotice({ type = "success", message, onClose, autoHideMs }) {
  useEffect(() => {
    if (autoHideMs && onClose) {
      const timer = setTimeout(onClose, autoHideMs);
      return () => clearTimeout(timer);
    }
  }, [autoHideMs, onClose]);

  if (!message) return null;

  const config = {
    success: {
      bg: "bg-secondary-container",
      text: "text-on-secondary-container",
      icon: "check_circle",
      iconColor: "text-secondary"
    },
    danger: {
      bg: "bg-error-container",
      text: "text-on-error-container",
      icon: "error",
      iconColor: "text-error"
    },
    warning: {
      bg: "bg-tertiary-container",
      text: "text-on-tertiary-container",
      icon: "warning",
      iconColor: "text-tertiary"
    }
  };

  const current = config[type] || config.success;

  return (
    <div 
      className={`flex items-center gap-3 px-4 py-3 rounded-2xl ${current.bg} ${current.text} shadow-sm border border-black/5 animate-in fade-in slide-in-from-top-2 duration-300`}
      role="alert"
    >
      <span className={`material-symbols-outlined ${current.iconColor} text-xl`} style={{ fontVariationSettings: "'FILL' 1" }}>
        {current.icon}
      </span>
      <span className="text-xs font-black uppercase tracking-widest flex-1">
        {message}
      </span>
      {onClose && (
        <button 
          type="button" 
          className="p-1 hover:bg-black/10 rounded-lg transition-colors" 
          onClick={onClose}
          aria-label="Close"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>
      )}
    </div>
  );
}
