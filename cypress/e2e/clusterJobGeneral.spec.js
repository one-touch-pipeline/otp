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

describe('Check cluster job general page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should click through the pages of the table', () => {
      cy.intercept('/clusterJobGeneral/findAllClusterJobsByDateBetween?*').as('dataTableSource');
      cy.intercept('/clusterJobGeneral/getAllStatesTimeDistribution?*').as('getAllStatesTimeDistribution');
      cy.intercept('/clusterJobGeneral/getAllExitCodes?*').as('getAllExitCodes');
      cy.intercept('/clusterJobGeneral/getAllExitStatuses?*').as('getAllExitStatuses');
      cy.intercept('/clusterJobGeneral/getAllStates?*').as('getAllStates');
      cy.intercept('/clusterJobGeneral/getAllFailed?*').as('getAllFailed');
      cy.intercept('/clusterJobGeneral/getAllAvgCoreUsage?*').as('getAllAvgCoreUsage');
      cy.intercept('/clusterJobGeneral/getAllMemoryUsage?*').as('getAllMemoryUsage');

      cy.visit('/clusterJobGeneral/index');

      //check all async loaded data
      cy.wait('@dataTableSource').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllStatesTimeDistribution').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllExitCodes').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllExitStatuses').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllStates').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllFailed').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllAvgCoreUsage').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllMemoryUsage').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      //fixed time to have some data
      cy.get('input#dpFrom').type('2022-08-20');
      cy.get('input#dpTo').type('2022-09-10');

      //check all async loaded data
      cy.wait('@dataTableSource').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllStatesTimeDistribution').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllExitCodes').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllExitStatuses').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllStates').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllFailed').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllAvgCoreUsage').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.wait('@getAllMemoryUsage').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      //go to page 2 checks
      cy.get('a.page-link').contains('2').click();
      cy.wait('@dataTableSource').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.get('div#clusterJobGeneralTable_processing').should('not.be.visible');

      //go to next page
      cy.get('a.page-link').contains('Next').click();
      cy.wait('@dataTableSource').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.get('div#clusterJobGeneralTable_processing').should('not.be.visible');

      //go again to page 1
      cy.get('a.page-link').contains('1').click();
      cy.wait('@dataTableSource').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/clusterJobGeneral/index');
      cy.checkAccessDenied('/clusterJobGeneral/findAllClusterJobsByDateBetween');
      cy.checkAccessDenied('/clusterJobGeneral/getAllStatesTimeDistribution');
      cy.checkAccessDenied('/clusterJobGeneral/getAllExitCodes');
      cy.checkAccessDenied('/clusterJobGeneral/getAllExitStatuses');
      cy.checkAccessDenied('/clusterJobGeneral/getAllStates');
      cy.checkAccessDenied('/clusterJobGeneral/getAllFailed');
      cy.checkAccessDenied('/clusterJobGeneral/getAllAvgCoreUsage');
      cy.checkAccessDenied('/clusterJobGeneral/getAllMemoryUsage');
    });
  });
});
