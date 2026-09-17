import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

const backendProxy = {
  target: 'http://localhost:11451',
  bypass(request) {
    // Frontend history routes can share paths with backend APIs.
    if (request.headers.accept?.includes('text/html')) return request.url
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 开发环境：API 代理到本地 Spring Boot 后端
    proxy: {
      '/user': backendProxy,
      '/sign': backendProxy,
      '/admin': backendProxy,
      '/announcement': backendProxy
    }
  },
  build: {
    // 新前端正式接管后，直接把产物输出到后端 static，实现单 jar 部署：
    // outDir: '../xsyu-sign-server/src/main/resources/static',
    // emptyOutDir: true
  }
})
