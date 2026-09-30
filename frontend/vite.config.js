import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Địa chỉ backend mà dev server proxy tới. Mặc định backend chạy local ở cổng 8080
// (.\scripts\run-backend.ps1). Muốn dùng backend trong Docker (cổng 8081):
//   $env:VITE_PROXY_TARGET = "http://localhost:8081"; npm run dev
const backend = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Lock dev server to required port and avoid auto change
    port: 5175,
    strictPort: true,
    proxy: {
      '/api': {
        target: backend,
        changeOrigin: true,
        secure: false,
      },
      '/ws': {
        target: backend,
        ws: true,
        changeOrigin: true,
        secure: false,
      },
      '/uploads': {
        target: backend,
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
