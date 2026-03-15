import { defineConfig } from 'cypress';

export default defineConfig({
      e2e: {
            baseUrl: 'http://localhost:5174',
            specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
            supportFile: 'cypress/support/e2e.ts',
            video: true,
            screenshotsFolder: 'cypress/screenshots',
            videosFolder: 'cypress/videos',
            viewportWidth: 1280,
            viewportHeight: 720
      },
      component: {
            specPattern: 'cypress/components/**/*.cy.{js,jsx,ts,tsx}',
            supportFile: 'cypress/support/components.ts',
            devServer: {
                  framework: 'vue',
                  bundler: 'vite'
            }
      }
});
