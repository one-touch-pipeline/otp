/*
 * Copyright 2011-2025 The OTP authors
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

const path = require('path');

const login = (username, password) => {
  'use strict';

  cy.session(username, () => {
    cy.clearCookies();
    cy.setCookie('CY_LOGIN_TIME', new Date().toString());

    // Login via endpoint
    cy.request({
      method: 'POST',
      url: '/authenticate',
      form: true,
      body: {
        username,
        password
      },
      followRedirect: false
    }).then((response) => {
      expect(response.status).to.eq(302);
      cy.getCookie('JSESSIONID').should('exist');
    });

    // Accept the privacy policy
    cy.request({
      method: 'POST',
      url: '/privacyPolicy/accept',
      form: true,
      body: {
        redirect: '',
        accept: 'on'
      },
      followRedirect: false
    }).its('status').should('eq', 302);
  }, {
    validate: validateSession,
    cacheAcrossSpecs: false
  });
};

/** Logging in as user, departmentHead, operator, departmentDeputy */
Cypress.Commands.add('loginAs', (type) => {
  'use strict';

  const username = Cypress.env(`${type}_username`);
  const password = Cypress.env(`${type}_password`);

  login(username, password);
});

Cypress.Commands.add('logout', () => {
  'use strict';

  cy.visit('/logout');
});

let checkedHrefList = [];
const excludedLinks = [];

// eslint-disable-next-line strict
Cypress.Commands.add('checkAllAnkerElements', (initial = true) => {
  if (initial) {
    checkedHrefList = [];
  }
  const ankerElementWithHref = 'a[href]:not([href=""])';
  if (Cypress.$(ankerElementWithHref).length === 0) {
    return;
  }
  cy.get(ankerElementWithHref)
    .each(($a) => {
      const href = $a.prop('href');
      const short = href.split('?')[0];
      const { baseUrl } = Cypress.config();
      if (href.includes(baseUrl) && !checkedHrefList.includes(short) && !excludedLinks.includes(short)) {
        checkedHrefList.push(short);
        cy.log(`Try http request for ${href} and visit browser page.`);
        cy.request(href)
          .then((response) => {
            if (response.headers['content-type'].includes('html')) {
              cy.visit(href);
            }
          });
        cy.checkAllAnkerElements(false);
      }
    });
});

Cypress.Commands.add('checkPage', (url) => {
  'use strict';

  if (url) {
    cy.url().should('contain', url).then((currentUrl) => {
      cy.request(currentUrl).its('status').should('equal', 200);
    });
  } else {
    cy.url().then((currentUrl) => {
      cy.request(currentUrl).its('status').should('equal', 200);
    });
  }
});

/**
 * checks download files
 *
 * check that
 * - a file exists with given name, the current date and the given ending
 * - the file has all the given headers in given order
 * - the files contains all the given lines, the order within the line needs to match (but not the order of all lines)
 *   and file may contain more lines
 *
 * HEADER1,HEADER2,HEADER3
 * v1a,v1b,v1c
 * v2a,v2b,v2c
 * v3a,v3b,v3c
 *
 * [[v3a,v3b,v3c],[v2a,v2b,v2c]]
 *
 */
// eslint-disable-next-line strict
Cypress.Commands.add('checkDownloadByContent', (filename, fileEnding, headerList, contentListList, quote = '"') => {
  const downloadsFolder = Cypress.config('downloadsFolder');
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  const date = `${year}-${month}-${day}`;

  const filepath = path.join(downloadsFolder, `${filename}_${date}${fileEnding}`);

  cy.readFile(filepath, 'utf8', { timeout: 5000 }).then((content) => {
    cy.log(`content: ${content}`);
    const lines = content.split('\n');
    cy.log('rows:');
    lines.forEach((line) => {
      cy.log(`line: ${line}`);
    });

    // check header
    const headerLine = lines[0];
    const headerLineExpected = `${quote}${headerList.join(`${quote},${quote}`)}${quote}`;
    cy.log(`Check header: ${headerLineExpected}`);
    cy.wrap(headerLine).should('equal', headerLineExpected);

    // check for expected content
    contentListList.forEach((contentList) => {
      const expectedContentLine = `${quote}${contentList.join(`${quote},${quote}`)}${quote}`;
      cy.log(`Check for content: ${expectedContentLine}`);
      cy.wrap(lines).should('include', expectedContentLine);
    });
  });
});

// eslint-disable-next-line strict
Cypress.Commands.add('checkDownloadByContentOfFixture', (fixtureFileName, quote = '"') => {
  cy.fixture(`downloadChecks/${fixtureFileName}`).then((config) => {
    cy.checkDownloadByContent(config.filename, config.extension, config.header, config.data, quote);
  });
});

Cypress.Commands.add('clearDownloadsFolder', () => {
  'use strict';

  const downloadsFolder = Cypress.config('downloadsFolder');

  cy.exec(`rm ${downloadsFolder}/*`, {
    log: true,
    failOnNonZeroExit: false
  });
});

Cypress.Commands.add('checkAccessDenied', (url) => {
  cy.checkAccessDenied(url, 403);
});

Cypress.Commands.add('checkAccessDenied', (url, statusCode) => {
  'use strict';

  cy.request({
    url,
    followRedirect: false,
    failOnStatusCode: false
  }).then((resp) => {
    expect(resp.status).to.be.eq(statusCode || 403);
  });
});

Cypress.Commands.add('checkNotFound', (url) => {
  'use strict';

  cy.request({
    url,
    followRedirect: false,
    failOnStatusCode: false
  }).then((resp) => {
    expect(resp.status).to.be.eq(404);
  });
});

/**
 * Command to check the input for correctness, to prevent flaky inputs
 */
Cypress.Commands.add('checkedTyping', (commandFn, input) => {
  'use strict';

  commandFn().clear().type(input).invoke('val')
    .then((currentValue) => {
      if (currentValue.toString() !== input.toString()) {
        cy.checkedTyping(commandFn, input);
      }
    });
});

/** Command to set a processing option for preparation of a test */
Cypress.Commands.add('setProcessingOption', (optionName, value) => {
  'use strict';

  cy.visit('/processingOption/index');
  cy.get('table tr').contains(optionName).parent().as('optionRow');

  cy.get('@optionRow').find('.form-control').then((element) => {
    if (element.val() !== value) {
      cy.intercept('/processingOption/update*').as('updateProcessingOption');
      cy.get('@optionRow').find('[title="Edit"]').click();

      if (element.get(0).tagName === 'SELECT') {
        cy.wrap(element).select(value, { force: true });
      } else {
        cy.wrap(element).type(value, { force: true });
      }

      cy.get('@optionRow').contains('Save').click({ force: true });
      cy.wait('@updateProcessingOption').its('response.statusCode').should('eq', 200);
    }
  });
});

/**
 * Intercept the number #index (1 based index) HTTP call with the same method and URL
 * E.g.: 3 HTTP requests are sent, but you want to intercept the 2nd one, then specify: index = 2
 * @param method can be GET, POST, PUT, DELETE, PATCH, default is GET
 * @param index is 1 based index
 */
Cypress.Commands.add('interceptAt', (alias, url, index, method) => {
  'use strict';

  let intercepted = 0;
  cy.intercept(method || 'GET', url, (req) => {
    intercepted += 1;
    if (intercepted === index) {
      req.alias = alias;
    }
  });
});

const validateSession = () => {
  'use strict';

  return new Promise((resolve, reject) => {
    cy.getCookie('CY_LOGIN_TIME').then((cookie) => {
      const loginTimeDiff = new Date() - new Date(cookie.value);
      if (loginTimeDiff > 900000) {
        reject();
      } else {
        cy.getCookie('JSESSIONID').then((jSessionCookie) => {
          if (jSessionCookie) {
            resolve();
          } else {
            reject();
          }
        });
      }
    });
  });
};

/**
 * Command to log to cypress and to the console.
 *
 * It is helpful for dubbing in the ci, since there the cypress logs are only partly available in the screenshot,
 * but not all before.
 */
Cypress.Commands.add('logBoth', (text) => {
  'use strict';

  cy.log(text);
  cy.task('log', text);
});

/**
 * Command for logging when a switch is turned on to avoid logging too much in the CI pipeline.
 *
 * The environment variable CYPRESS_LOG_DEBUG_ENABLED must be set to true to enable it.
 * This could be useful for performing local cypress test.
 */
Cypress.Commands.add('logDebug', (message, ...args) => {
  if (Cypress.env('LOG_DEBUG_ENABLED')) {
    return cy.log(`debug: ${message}`, args);
  }
  return undefined;
});

/**
 * Command to toggle a button and check the expected result
 * @param {string} buttonText - The text content of the button to find
 * @param {string} expectedSelector - The selector to verify after clicking
 * @param {(currentSubject: JQuery<HTMLElement>) => void} assertionCallback - Callback for assertions
 * @example
 * cy.toggleButton('Show Edit', '.edit-switches', ($el) => {
 *   expect($el).to.be.visible;
 * });
 */
Cypress.Commands.add('toggleButton', (buttonText, expectedSelector, assertionCallback) => {
  'use strict';

  return cy.contains('button', buttonText)
    .should('exist')
    .click()
    .then(() => {
      cy.get(expectedSelector)
        .should('exist').should(assertionCallback);
    });
});
