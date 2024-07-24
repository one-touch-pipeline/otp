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

describe('Check blacklisted ILSe Numbers page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/metadataImport/blacklistedIlseNumbers');
    });


    // this test is a one time test, because in each subsequent run the numbers are already blacklisted
    it('blacklist ilse numbers with different delimiters', function() {
      cy.get('#ilse').type('54321, 54320; 54319 54318\t54317');
      cy.get('#comment').type('test delimiters');
      cy.get('#addButton').click();

      cy.get('tbody tr').should('have.length', 6);
      ['54321', '54320', '54319', '54318', '54317'].forEach((number) => {
        cy.get('tr > td:first-child').should('contain.text', number);
        cy.get('tr').contains(number).parent().should('contain.text', 'test delimiters');
      });

      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'The number was stored successfully');
    });


    it('remove an ilse number from blacklist', function() {
      cy.get('tbody tr').its('length').as('initialLength');

      cy.intercept('/metadataImport/unBlacklistIlseSubmissions*').as('unBlacklistIlseSubmissions');
      cy.get('tr:contains("test delimiters") #unBlacklist').click();

      cy.wait('@unBlacklistIlseSubmissions').its('response.statusCode').should('eq', 302);
      cy.get('tbody tr').should(function(rows) {
        expect(rows).to.have.length(this.initialLength - 1);
      });

      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'Removed ILSe number from the blacklist successfully');
    });


    it('should not be able to add an already blacklisted ilse number', function() {
      cy.get('#ilse').type('54321');
      cy.get('#comment').type('test already added');
      cy.get('#addButton').click();

      cy.get('#otpToastBox .otpErrorToast').should('exist')
        .and('contain.text', 'The ILSe number "54321" already exist. Only new ILSe numbers can be blacklisted.');
    });


    it('should not be able to add an invalid ilse number', function() {
      cy.get('#ilse').type('0, 9999999, test, 123, 123');
      cy.get('#comment').type('test invalid number');
      cy.get('#addButton').click();

      cy.get('#otpToastBox .otpErrorToast').should('exist')
        .should('contain.text', 'The value "0" is too small')
        .should('contain.text', 'The value "9999999" is too big')
        .should('contain.text', 'The value "test" is not a number.')
        .should('contain.text', 'The ILSe number "123" is more than once in the input list.');
    });


    it('should not be able to add an ilse number without comment', function() {
      cy.get('#ilse').type('12345');
      cy.get('#addButton').click();

      cy.get('#otpToastBox .otpErrorToast').should('exist')
        .should('contain.text', 'Field "comment" cannot be empty');
      cy.get('tr > td:first-child').should('not.contain', 12345);
    });


    it('should edit comment of blacklisted ilse', function() {
      cy.intercept('/metadataImport/saveComment/*').as('updateBlacklistComment');

      cy.get('table#blacklistIlseTable tbody tr').filter(':contains("test delimiters")').first().as('blacklistIlseRow');

      // adding information to the comment
      cy.get('@blacklistIlseRow').find('.edit-button-left').click();
      cy.get('@blacklistIlseRow').find('.edit-switch-input').type(' edited');
      cy.get('@blacklistIlseRow').find('button.save').click();

      cy.wait('@updateBlacklistComment').its('response.statusCode').should('eq', 200);
      cy.get('@blacklistIlseRow').should('contain.text', 'test delimiters edited');

      // removing information from the comment
      cy.get('@blacklistIlseRow').find('.edit-button-left').click();
      cy.get('@blacklistIlseRow').find('.edit-switch-input').clear().type('test delimiters');
      cy.get('@blacklistIlseRow').find('button.save').click();

      cy.wait('@updateBlacklistComment').its('response.statusCode').should('eq', 200);
      cy.get('@blacklistIlseRow').should('contain.text', 'test delimiters');
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/metadataImport/blacklistedIlseNumbers');
    });
  });
});
