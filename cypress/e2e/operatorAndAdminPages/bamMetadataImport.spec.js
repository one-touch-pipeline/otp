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

describe('Check bam metadata import page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/bamMetadataImport/index');
    });

    it('should import bam metadata', () => {
      const ticketNumber = 1234
      const filePath = '/home/otp/filesystem/bam-import/import.tsv'
      cy.intercept('/bamMetadataImport/validateOrImport*').as('validateOrImport');

      cy.get('#path').clear().type(filePath);
      cy.get('input#ticketNumber').type(ticketNumber);
      
      cy.get('input#validate').click();

      cy.wait('@validateOrImport').then((interception) => {
        expect(interception.request.body).to.contain(filePath.replaceAll('/','%2F'))
        expect(interception.request.body).to.contain(ticketNumber)

        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /\/\/?bamMetadataImport\/index/);

        cy.get('#ignoreWarnings').check();
        cy.get('input#import').click();

        cy.wait('@validateOrImport').then((interception2) => {
          expect(interception2.response.statusCode).to.eq(302);
          cy.location('pathname').should('match', /\/\/?sampleOverview\/index/);
        });
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/bamMetadataImport/index');
      cy.checkAccessDenied('/bamMetadataImport/validateOrImport');
    });
  });
});
