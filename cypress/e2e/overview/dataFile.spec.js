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

describe('Check rawSequenceFile pages', () => {
  'use strict';

  const userRoles = ['operator', 'user'];

  userRoles.forEach((userRole) => {
    context(`when user is ${userRole}`, () => {
      beforeEach(() => {
        cy.loginAs(userRole);
      });

      it('should visit show details page by starting on project overview page and save a comment there', () => {
        cy.intercept('/projectOverview/dataTableSourceSequencingSamples?project=*').as('dataTableSource');

        // Only intercept save comment for operator role
        if (userRole === 'operator') {
          cy.intercept('/rawSequenceFile/saveDataFileComment*').as('saveDataFileComment');
        }

        cy.visit('/projectOverview/index');
        cy.wait('@dataTableSource').then((interception) => {
          expect(interception.response.statusCode).to.be.eq(200);

          cy.get('table#projectOverviewTable tbody tr').first()
            .find('td')
            .first()
            .click();

          cy.get('table tbody tr').find('a').eq(2)
            .click();
          cy.checkPage('seqTrack/seqTrackSet');

          cy.get('div.identifier.dataFile a').eq(2).click();
          cy.checkPage('rawSequenceFile/showDetails');

          if (userRole === 'operator') {
          // Operator can edit comments
            const comment = `This is a random ${Cypress._.random(0, 1e6)} comment.`;
            cy.get('textarea#comment-content').clear();
            cy.get('textarea#comment-content').type(comment);
            cy.get('button#button-save').click();

            cy.wait('@saveDataFileComment').then((intercept) => {
              expect(intercept.response.statusCode).to.be.eq(200);
              cy.get('span#authorSpan').should('not.be.empty');
            });
          } else {
          // User can only view - verify page loads and comment section is visible but readonly
            cy.get('textarea#comment-content').should('exist');
            cy.get('textarea#comment-content').should('have.attr', 'readonly');
          }
        });
      });
    });
  });
});
