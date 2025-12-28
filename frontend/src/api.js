// src/api.js
import axios from "axios";

const IS_BROWSER = typeof window !== "undefined";
const IS_LOCALHOST = IS_BROWSER
  ? window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
  : false;
const DEV_API_BASE_URL = "http://localhost:8080/api";
const USE_DIRECT_BACKEND = IS_BROWSER && IS_LOCALHOST && window.location.port === "5173";
const VITE_DEV_FLAG = import.meta?.env?.DEV;
const IS_DEV = VITE_DEV_FLAG === true || USE_DIRECT_BACKEND;
const API_BASE_URL = USE_DIRECT_BACKEND ? DEV_API_BASE_URL : "/api";

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Chuẩn hóa xử lý lỗi
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || "";
    const responseData = error?.response?.data;
    const contentType = error?.response?.headers?.["content-type"] || "";
    const isHtmlResponse =
      typeof responseData === "string" &&
      (responseData.includes("<html") ||
        responseData.includes("<!DOCTYPE html") ||
        responseData.includes("Method 'POST' is not supported"));
    const effectiveBaseURL =
      error?.config?.baseURL || error?.config?.__apiBaseURL || api.defaults.baseURL;
    const canRetryDirect =
      IS_LOCALHOST &&
      !error?.config?.__directBaseTried &&
      error?.config &&
      effectiveBaseURL === "/api" &&
      (status === 404 || status === 405) &&
      (isHtmlResponse ||
        contentType.includes("text/html") ||
        String(responseData || "").includes("Method 'POST' is not supported"));

    if (canRetryDirect) {
      try {
        return await api.request({
          ...error.config,
          baseURL: DEV_API_BASE_URL,
          __directBaseTried: true,
        });
      } catch (retryError) {
        error = retryError;
      }
    }
    // Không redirect đối với các endpoint auth (đặc biệt /auth/login)
    const isAuthEndpoint = url.includes("/auth/");

    if (!isAuthEndpoint && (status === 401 || status === 403)) {
      try {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
      } catch { void 0; }
      if (window.location?.pathname !== "/login") {
        window.location.assign("/login");
      }
    }
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      "Yêu cầu thất bại";
    const normalizedError = new Error(message);
    normalizedError.status = error?.response?.status ?? status;
    normalizedError.url =
      (error?.config?.baseURL || error?.config?.__apiBaseURL || "") + (error?.config?.url ?? url);
    normalizedError.data = error?.response?.data;
    return Promise.reject(normalizedError);
  }
);

// Đính kèm Authorization nếu có token
api.interceptors.request.use((config) => {
  if (IS_DEV && !config.baseURL) {
    config.baseURL = API_BASE_URL;
  }
  config.__apiBaseURL = config.baseURL || api.defaults.baseURL;
  const token = localStorage.getItem("token");
  if (token) {
    config.headers = config.headers || {};
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

export default api;
