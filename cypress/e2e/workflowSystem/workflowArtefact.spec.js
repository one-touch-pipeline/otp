/*
 * Copyright 2011-2024 The OTP authors
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

describe('Check workflow artefact page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should visit different workflow artefacts index pages for a succeeded workflow', () => {
      cy.intercept('/workflowRunList/data*').as('data');
      cy.visit('/workflowRunList');
      cy.wait('@data').its('response.statusCode').should('eq', 200);

      cy.get('select#state').select('SUCCESS', { force: true });
      cy.wait('@data').its('response.statusCode').should('eq', 200);

      cy.get('input#name').type('example_2{enter}');
      cy.wait('@data').its('response.statusCode').should('eq', 200);

      cy.get('table#runs tbody').find('tr')
        .eq(2)
        .find('a')
        .click();

      cy.get('a:contains(artefact)').each((artefactAnkerElement) => {
        cy.visit(artefactAnkerElement.prop('href'));
        cy.checkPage('/workflowArtefact/index/');
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowArtefact/index');
    });
  });
});
