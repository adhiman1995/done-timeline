import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Relative asset paths so the build works under https://<user>.github.io/<repo>/
  base: './',
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:3001' },
  },
});
