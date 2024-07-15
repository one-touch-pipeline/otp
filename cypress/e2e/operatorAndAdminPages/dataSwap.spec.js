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
describe('test for data swap page', () => {
  'use strict';

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });
    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/dataSwap');
    });
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should download data swap template from sequence page and upload on data swap page', () => {
      cy.visit('/sequence');
      cy.intercept('/sequence/sampleSwapTemplate*').as('downloadTemplate');
      cy.intercept('/dataSwap/swapData*').as('dataSwap');

      cy.get('#sequenceTable_processing').should('not.be.visible');
      cy.get('div#sequenceTable_wrapper button').contains('Download Sample Swap Template').click();
      cy.wait('@downloadTemplate').then((res) => {
        cy.visit('/dataSwap');
        const filename = res.response.headers['content-disposition'].split('filename=')[1];
        const downloadsFolder = Cypress.config('downloadsFolder');

        cy.get('#data-swap-file').selectFile(`${downloadsFolder}/${filename}`);
      });
      cy.get('#data-swap-btn').click();

      cy.wait('@dataSwap').then((interception) => {
        expect(interception.response.statusCode).eq(200);
        expect(interception.response.body.problems.length).eq(0);
      });

      cy.get('.otpSuccessToast').should('exist').and('contain.text', 'Successfully');
    });

    it('should show an error for data swap files with problems', () => {

      cy.intercept('/dataSwap/swapData*').as('dataSwap');

      cy.visit('/dataSwap');
      const fixturesFolder = Cypress.config('fixturesFolder');

      cy.get('#data-swap-file').selectFile(`${fixturesFolder}/file-uploads/failing-data-swap-file.csv`);

      cy.get('#data-swap-btn').click();

      cy.wait('@dataSwap').then((interception) => {
        expect(interception.response.statusCode).eq(200);
        expect(interception.response.body.problems.length).eq(1);
      });
      cy.get('.otpWarningToast').should('exist').and('contain.text', 'Problems');
    });
  });
});
