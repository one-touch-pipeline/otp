/*
 * Copyright 2011-2023 The OTP authors
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

Cypress.Commands.add('loginAsUser', () => {
  'use strict';

  const username = Cypress.env('user_username');
  const password = Cypress.env('user_password');

  login(username, password);
});

Cypress.Commands.add('loginAsDepartmentHeadUser', () => {
  'use strict';

  const username = Cypress.env('departmentHead_username');
  const password = Cypress.env('departmentHead_password');

  login(username, password);
});

Cypress.Commands.add('loginAsOperator', () => {
  'use strict';

  const username = Cypress.env('operator_username');
  const password = Cypress.env('operator_password');

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
      if (href.includes(Cypress.config().baseUrl) && !checkedHrefList.includes(href) && !excludedLinks.includes(href)) {
        checkedHrefList.push(href);
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

// eslint-disable-next-line strict
Cypress.Commands.add('checkDownloadByContent', (filename, fileEnding, contentList) => {
  const downloadsFolder = Cypress.config('downloadsFolder');
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  const date = `${year}-${month}-${day}`;

  const filepath = path.join(downloadsFolder, `${filename}_${date}${fileEnding}`);

  cy.readFile(filepath, 'utf8', { timeout: 5000 }).then((content) => {
    contentList.forEach((expectedContent) => {
      cy.log(`Check ${expectedContent}`);
      cy.wrap(content).should('contain', expectedContent);
    });
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
  'use strict';

  cy.request({
    url,
    followRedirect: false,
    failOnStatusCode: false
  }).then((resp) => {
    expect(resp.status).to.be.eq(403);
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


const validateSession = () => {
  'use strict';

  return new Promise((resolve, reject) => {
    cy.getCookie('CY_LOGIN_TIME').then((cookie) => {
      const loginTimeDiff = new Date() - new Date(cookie.value);
      if (loginTimeDiff > 900000) {
        reject();
      }
      resolve();
    });
  });
};
