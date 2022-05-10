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

Cypress.Commands.add('loginAsOperator', () => {
  'use strict';

  cy.visit('/');
  cy.get('body')
    .then(($body) => {
      const loginButtonId = '#loginButton';
      if ($body.find(loginButtonId).length > 0) {
        cy.get('input[name=username]')
          .type(Cypress.env('operator_username'));
        cy.get('input[name=password]')
          .type(Cypress.env('operator_password'), { log: false });
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

Cypress.Commands.add('logout', () => {
  'use strict';

  cy.visit('/');
  cy.get('body')
    .then((body) => {
      if (body.find('#loginButton').length === 0) {
        cy.get('a')
          .contains('Logout')
          .click();
      }
    });
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
