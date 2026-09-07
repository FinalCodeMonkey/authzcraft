import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/demo-api': {
        target: 'http://localhost:18080',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/demo-api/, ''),
      },
      '/center-api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/center-api/, ''),
      },
    },
  },
})