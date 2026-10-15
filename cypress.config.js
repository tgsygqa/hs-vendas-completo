const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    // O frontend é servido por "npm run start:front" (pasta /frontend, porta 5600).
    baseUrl: 'http://localhost:5600',
    specPattern: 'cypress/e2e/**/*.cy.js',
    supportFile: 'cypress/support/e2e.js',
    video: false,
    screenshotOnRunFailure: true,
    defaultCommandTimeout: 8000,
    // Um reteste automático só no modo headless (cada teste de Vendas começa
    // zerando a base, então refazer um teste é seguro).
    retries: { runMode: 1, openMode: 0 },
    env: {
      // API do backend Spring Boot (testes da suíte de Vendas).
      apiUrl: 'http://localhost:8080/api',
    },
  },
});
