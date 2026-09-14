import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
// import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  base: '/',
  plugins: [
    react(),
    // VitePWA({ // Uncomment to allow PWA
    //   registerType: 'autoUpdate',
    //   includeAssets: ['icons/*.png']
    // })
  ],
  server: {
    port: 6080
  },
  optimizeDeps: {
    include: ['react', 'react-dom'],
    force: true
  },
  test: {
    globals: true,
    environment: "jsdom",
    env: {
      VITE_AUTHSERVICE_INTEGRATION: "true",
      VITE_EMAILSERVICE_INTEGRATION: "true",
      VITE_FILESERVICE_INTEGRATION: "true",
      VITE_VAULTSERVICE_INTEGRATION: "true",
      VITE_GATEWAYSERVICE_INTEGRATION: "true"
    },
    setupFiles: "./src/test/setupTests.js", // optional: like jest setup
    include: ["src/test/**/*.{test,spec}.{js,jsx}"]
  },
})
