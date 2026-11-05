import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    // host: true 让开发服务器监听 0.0.0.0，手机连同一局域网就能访问：
    //   http://<电脑的局域网 IP>:5173
    // 注意：手机通过 HTTP + IP 访问时不是"安全上下文"，
    //       Service Worker（PWA 安装）不会生效，但页面布局、播放都正常。
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 本地音乐库：后端把配置的音乐目录挂载在 /library/** 下
      '/library': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
