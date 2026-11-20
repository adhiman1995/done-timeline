import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Relative asset paths so the build works under https://<user>.github.io/<repo>/
  base: './',
  server: { port: 5173 },
  build: {
    rollupOptions: {
      output: {
        // Keep Firebase in its own file so it stays cached between app updates.
        manualChunks: id => (id.includes('node_modules/firebase') || id.includes('node_modules/@firebase') ? 'firebase' : undefined),
      },
    },
    chunkSizeWarningLimit: 800,
  },
});
