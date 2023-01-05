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

describe('Check metadata import page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/metadataImport/index');
    });

    it('should import data, when required values are set', () => {
      cy.intercept('/metadataImport/validateOrImport*').as('validateOrImport');

      cy.get('#ticketNumber').clear().type('123');
      cy.get('#paths').clear().type('/home/otp/filesystem/otp_example_data/example_import_project01_1.csv');
      cy.get('input[type=radio][name=directoryStructure]').check('ABSOLUTE_PATH');
      cy.get('input#validate').click();

      cy.wait('@validateOrImport').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /\/\/?metadataImport\/index/);

        cy.get('#ignoreWarnings').check();
        cy.get('input#import').click();

        cy.wait('@validateOrImport').then((interception2) => {
          expect(interception2.response.statusCode).to.eq(302);
          cy.location('pathname').should('match', /\/\/?metadataImport\/details\/\d+/);
        });
      });
    });
  });
});
