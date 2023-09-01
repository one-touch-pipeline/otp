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

describe('Check projectInfo page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/projectInfo/list');
    });

    it('should add project info document', () => {
      cy.intercept('/projectInfo/addProjectInfo*').as('addProjectInfo');

      cy.get('input[name=projectInfoFile]').selectFile('cypress/fixtures/file-uploads/hello-world.txt');
      cy.get('input#Add').click();

      cy.wait('@addProjectInfo').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('eq', '/projectInfo/list');
        cy.get('h2').contains('Documents').next().should('contain.text', 'hello-world.txt');
      });
    });

    it('should add a comment to the file', () => {
      const comment = 'my-custom-comment';

      cy.intercept('/projectInfo/updateProjectInfoComment*').as('updateProjectInfoComment');

      cy.get('button.js-edit').first().click();
      cy.get('textarea[name=value]').type(comment);
      cy.get('button.save').click();

      cy.wait('@updateProjectInfoComment').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('.comment-box-wrapper').should('contain', comment);
      });
    });

    it('should delete the file', () => {
      cy.intercept('/projectInfo/deleteProjectInfo*').as('deleteProjectInfo');

      cy.get('input[name=permanentlyDelete]').click();
      cy.on('window:confirm', () => true);
      cy.on('window:confirm', () => true);

      cy.wait('@deleteProjectInfo').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('eq', '/projectInfo/list');
        cy.get('h2').contains('Documents').next().should('contain.text', 'There are no documents for this project');
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/projectInfo/list');
      cy.checkAccessDenied('/projectInfo/addProjectInfo');
      cy.checkAccessDenied('/projectInfo/updateProjectInfoComment');
      cy.checkAccessDenied('/projectInfo/deleteProjectInfo');
    });
  });
});
