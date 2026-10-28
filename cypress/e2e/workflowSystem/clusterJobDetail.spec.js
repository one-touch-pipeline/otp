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

describe('Check cluster job detail page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should visit the cluster job detail page via the cluster job general page', () => {
      cy.intercept('/clusterJobGeneral/findAllClusterJobsByDateBetween?*').as('dataTableSource');
      cy.visit('/clusterJobGeneral/index');
      cy.wait('@dataTableSource').its('response.statusCode').should('eq', 200);
      // typing a date sends the change event only when the input is deselected, and every change reloads the table to
      // the same time range as on the cluster job general page, which contains finished jobs
      cy.get('input#dpFrom').type('2022-08-20').blur();
      cy.wait('@dataTableSource').its('response.statusCode').should('eq', 200);
      cy.get('input#dpTo').type('2022-09-10').blur();
      cy.wait('@dataTableSource').its('response.statusCode').should('eq', 200);
      cy.get('div#clusterJobGeneralTable_processing').should('not.be.visible');

      cy.get('table tbody').find('tr a').each((clusterJobDetailAnkerLink) => {
        cy.visit(clusterJobDetailAnkerLink.prop('href'));
        cy.checkPage('/clusterJobDetail/show');
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/clusterJobGeneral/index');
      cy.checkAccessDenied('/clusterJobDetail/show');
    });
  });
});
