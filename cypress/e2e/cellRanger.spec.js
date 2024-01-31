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

describe('Check cell ranger page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should visit the final run selection page', () => {
      cy.visit('/cellRanger/finalRunSelection');
    });

    it('should select multiple run configuration and save these', () => {
      cy.visit('/cellRanger/finalRunSelection');
      cy.intercept('/cellRanger/saveFinalRunSelection*').as('saveRunSelection');

      // Select second run selection to use default
      cy.get('input#input-3').click();

      // Select third run selection to not keep any runs
      cy.get('input#input-6').click();

      // Save run configurations
      cy.get('input#save').click();

      cy.wait('@saveRunSelection').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });

      cy.get('#cell-ranger-run-table tbody tr').eq(2).contains('Final run');
      cy.get('#cell-ranger-run-table tbody tr').eq(3).contains('Deleted run');
    });

    it('should delete the created final run', () => {
      cy.visit('/cellRanger/finalRunSelection');
      cy.intercept('/cellRanger/deleteFinalSelectedRun*').as('deleteFinalRun');

      cy.get('#cell-ranger-run-table tbody tr').eq(2).find('button.delete-btn').click();
      cy.get('#confirmDeleteModal').should('be.visible').find('button.confirm').click();

      cy.wait('@deleteFinalRun').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });

      cy.get('#cell-ranger-run-table tbody tr').eq(2).contains('Deleted run');
    });
  });
});
