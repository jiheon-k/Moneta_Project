import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxy = {
    '/api': {
      target: env.API_PROXY_TARGET || (mode === 'demo' ? 'http://127.0.0.1:8081' : 'http://127.0.0.1:8080'),
      changeOrigin: true,
      rewrite: (path: string) => path.replace(/^\/api/, ''),
    },
  }

  return {
    plugins: [react()],
    server: { host: 'localhost', port: mode === 'demo' ? 5174 : 5173, strictPort: true, proxy },
    preview: { host: 'localhost', port: 4173, strictPort: true, proxy },
  }
})
