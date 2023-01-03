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

describe('Check metadata import details page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.intercept('/sequence/dataTableSource*').as('dataSource');
      cy.intercept('/run/show/*').as('runDetails');
      cy.intercept('/metadataImport/details/*').as('metadataImportDetails');

      cy.loginAsOperator();
      cy.visit('/sequence/index');

      cy.wait('@dataSource').then(() => {
        cy.get('a[title=run_0]').click();
        cy.wait('@runDetails').then(() => {
          cy.get('.metaDataDetails-link').first().find('a').click();

          cy.wait('@metadataImportDetails');
        });
      });
    });

    it('should save a comment', () => {
      cy.intercept('/metadataImport/updateSeqCenterComment*').as('saveComment');

      cy.get('.comment-td').find('button.js-edit').click();
      cy.get('.comment-td').find('textarea.edit-switch-input').type('my test comment');
      cy.get('.comment-td').find('button.save').click();

      cy.wait('@saveComment').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#infoBox').find('.message').should('contain.text', 'Success');
      });
    });
  });
});
