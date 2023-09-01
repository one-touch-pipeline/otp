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

describe('Check workflow run list page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/workflowRunList/index?state=FAILED');
    });

    it('should filter the list by state', () => {
      cy.intercept('/workflowRunList/data*').as('workflowRunListFilter');

      const successClass = 'green';
      const failedClass = 'red';

      cy.get('table#runs .dot').should('have.class', failedClass);
      cy.get('table#runs .dot').should('not.have.class', successClass);

      cy.get('select#state').select('SUCCESS', { force: true });

      cy.wait('@workflowRunListFilter').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('table#runs .dot').should('have.class', successClass);
        cy.get('table#runs .dot').should('not.have.class', failedClass);
      });
    });

    it('should set a failed workflow to failed final', () => {
      cy.get('td button.failed-final-btn').first().click();
      cy.get('.toast-body').should('contain.text', 'success');
    });

    it('should restart a failed workflow step', () => {
      cy.get('td button.restart-step-btn').last().click();
      cy.get('.toast-body').should('contain.text', 'success');
    });

    it('should restart the previous step of a failed workflow', () => {
      cy.get('button.dropdown-toggle').last().click();
      cy.get('button.dropdown-item').last().click();
      cy.get('.toast-body').should('contain.text', 'success');
    });

    it('should restart a failed workflow run', () => {
      cy.get('td button.restart-run-btn').first().click();
      cy.get('.toast-body').should('contain.text', 'success');
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowRunList/index');
      cy.checkAccessDenied('/workflowRunList/data');
    });
  });
});
