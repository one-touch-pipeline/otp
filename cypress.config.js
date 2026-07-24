/*
 * Copyright 2011-2026 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
const { defineConfig } = require('cypress');

module.exports = defineConfig({
  // Disallow the deprecated, insecure Cypress.env() browser API. Sensitive values are read via
  // cy.env() and public values via Cypress.expose() instead. See https://on.cypress.io/cypress-env-migration
  allowCypressEnv: false,
  e2e: {
    baseUrl: 'http://localhost:8080/',
    specPattern: 'cypress/e2e/**/*.spec.js',
    supportFile: 'cypress/support/index.js',
    experimentalRunAllSpecs: true,
    testIsolation: false,
    // eslint-disable-next-line strict
    setupNodeEvents(on, config) {
      // Load env vars from cypress/cypress.env.json (canonical location alongside other cypress config).
      // Split them so only non-sensitive values reach the browser: usernames are public and accessed
      // synchronously via Cypress.expose(); passwords and secrets stay sensitive and are read via the
      // async cy.env() command. See https://on.cypress.io/cypress-env-migration
      // eslint-disable-next-line global-require
      const envFile = require('./cypress/cypress.env.json');
      const isPublic = (key) => key.endsWith('_username');

      config.env = { ...config.env };
      config.expose = { ...config.expose };
      Object.entries(envFile).forEach(([key, value]) => {
        if (isPublic(key)) {
          config.expose[key] = value;
        } else {
          config.env[key] = value;
        }
      });
      // Optional local debug toggle, formerly read via Cypress.env('LOG_DEBUG_ENABLED').
      config.expose.LOG_DEBUG_ENABLED = process.env.CYPRESS_LOG_DEBUG_ENABLED;

      on('task', {
        log(message) {
          // eslint-disable-no-console
          console.log(message);
          return null;
        }
      });

      return config;
    }
  },
  screenshotsFolder: 'cypress/screenshots',
  fixturesFolder: 'cypress/fixtures',
  downloadsFolder: 'cypress/downloads',
  pageLoadTimeout: 120000,
  defaultCommandTimeout: 30000,
  timeout: 30000,
  reporter: 'cypress-multi-reporters',
  reporterOptions: {
    configFile: 'cypress/reporter-config.json',
    toConsole: true
  },
  responseTimeout: 30000,
  viewportHeight: 900,
  viewportWidth: 1440,
  video: false,
  videosFolder: 'cypress/videos',
  watchForFileChanges: false
});
