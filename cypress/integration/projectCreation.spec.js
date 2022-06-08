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

describe('Check projectCreation page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/projectCreation/index');
    });

    it('should visit the index page', () => {});

    it('should create project, when required values are set', () => {
      const randomString = Cypress._.random(0, 1e6);

      cy.intercept('/projectCreation/save*').as('saveProject');

      cy.get('#name').clear().type(`project-${randomString}`);
      cy.get('#individualPrefix').clear().type(`abc-${randomString}`);
      cy.get('#dirName').clear().type(`abc-${randomString}`);
      cy.get('#dirAnalysis').clear().type('/tmp/abc');
      cy.get('#unixGroup').clear().type('otp');
      cy.get('#description').clear().type(`Sample description: ${randomString}`);
      cy.get('#sendProjectCreationNotification').uncheck();
      cy.get('input#save').click();
      cy.get('#ignoreUsersFromBaseObjects').check();
      cy.get('input#save').click();

      cy.wait('@saveProject').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('eq', '/projectConfig/index');
      });
    });
  });
});
