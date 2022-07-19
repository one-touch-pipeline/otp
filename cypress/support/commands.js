/*
 * Copyright 2011-2022 The OTP authors
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
const md5 = require('md5');

const login = (username, password) => {
  'use strict';

  cy.session(username, () => {
    cy.visit('/login/auth');
    cy.get('body')
      .then(($body) => {
        const loginButtonId = '#loginButton';
        if ($body.find(loginButtonId).length > 0) {
          cy.get('input[name=username]')
            .type(username);
          cy.get('input[name=password]')
            .type(password, { log: false });
          cy.get(loginButtonId)
            .click();
        }
      });
    cy.get('body')
      .then(($body) => {
        const acceptPrivacyPolicy = 'input#accept';
        if ($body.find(acceptPrivacyPolicy).length > 0) {
          cy.get(acceptPrivacyPolicy)
            .click();
          cy.get('button')
            .contains('Continue')
            .click();
        }
      });
  });
};

Cypress.Commands.add('loginAsUser', () => {
  'use strict';

  const username = Cypress.env('user_username');
  const password = Cypress.env('user_password');

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
      cy.request(currentUrl);
    });
  } else {
    cy.url().then((currentUrl) => {
      cy.request(currentUrl);
    });
  }
});

// eslint-disable-next-line strict
Cypress.Commands.add('checkDownloadByMd5Sum', (filename, fileEnding, md5Sum) => {
  const downloadsFolder = Cypress.config('downloadsFolder');
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  const date = `${year}-${month}-${day}`;

  const filepath = path.join(downloadsFolder, `${filename}_${date}${fileEnding}`);

  cy.readFile(filepath, 'binary', { timeout: 15000 })
    .should((buffer) => expect(md5(buffer)).to.be.equal(md5Sum));
});

Cypress.Commands.add('clearDownloadsFolder', () => {
  'use strict';

  const downloadsFolder = Cypress.config('downloadsFolder');

  cy.exec(`rm ${downloadsFolder}/*`, {
    log: true,
    failOnNonZeroExit: false
  });
});
