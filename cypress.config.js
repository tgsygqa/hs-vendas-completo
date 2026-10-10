const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    // O frontend é servido por "npm run start:front" (pasta /frontend, porta 5500).
    baseUrl: 'http://localhost:5500',
    specPattern: 'cypress/e2e/**/*.cy.js',
    supportFile: 'cypress/support/e2e.js',
    video: false,
    screenshotOnRunFailure: true,
  },
});
