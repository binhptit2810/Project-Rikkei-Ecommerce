import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Proxy API calls để tránh CORS khi chạy local
    proxy: {
      '/api': {
        target: process.env.VITE_API_BASE_URL || 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path, // giữ nguyên path /api/v1/...
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  }
})
