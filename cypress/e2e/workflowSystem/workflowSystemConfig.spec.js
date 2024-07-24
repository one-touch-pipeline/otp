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

describe('Check workflow system configuration page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/workflowSystemConfig/index');
    });

    it('should update a workflow configuration', () => {
      const priority = Math.floor(Math.random() * 1000);
      const maxRuns = Math.floor(Math.random() * 10);
      const supportedSeqType = 'ChIP PAIRED bulk';

      cy.intercept('/workflowSystemConfig/updateWorkflow*').as('updateWorkflow');

      cy.get('td').contains('Cell Ranger').siblings().last()
        .find('button i.bi-pencil')
        .click();

      cy.get('#editWorkflowModal').should('be.visible');
      cy.get('#editWorkflowModal').find('.modal-title').should('contain.text', 'Cell Ranger');

      cy.checkedTyping(() => cy.get('#editWorkflowModal #modal-priority'), priority);
      cy.checkedTyping(() => cy.get('#editWorkflowModal #modal-max-runs'), maxRuns);
      cy.get('#editWorkflowModal #modal-seqTypes').select(supportedSeqType, { force: true });

      cy.get('#editWorkflowModal #confirmModal').click();

      cy.wait('@updateWorkflow').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('td').contains('Cell Ranger').siblings().contains(priority);
        cy.get('td').contains('Cell Ranger').siblings().contains(maxRuns);
        cy.get('td').contains('Cell Ranger').siblings().contains(supportedSeqType);
      });
    });

    it('should edit a workflow version with comment and deprecated it and finally undeprecate it again', () => {
      const comment = 'This is a test comment. With some numbers 124 and signs %&!.';
      const supportedSeqType = 'ChIP PAIRED bulk';
      const allowedRefGen = 'GRCm38mm10_PhiX';

      cy.intercept('/workflowSystemConfig/getWorkflowVersions*').as('getWorkflowVersions');
      cy.intercept('/workflowSystemConfig/updateWorkflowVersion*').as('updateWorkflowVersion');

      cy.get('td').contains('PanCancer alignment').siblings().first().as('wvRow');

      cy.get('@wvRow').find('button i#versions-icon').click();
      cy.wait('@getWorkflowVersions').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        const workflowVersionId = interception.response.body[0].id;
        cy.get(`#modify-btn-${workflowVersionId}`).should('be.visible').as('deprecateBtn');

        // deprecate workflow version
        cy.get('@deprecateBtn').click();
        cy.get('#updateWorkflowVersionModal').as('modal').should('be.visible');

        cy.get('#updateWorkflowVersionModal #modal-seqTypes-version').select(supportedSeqType, { force: true });
        cy.get('#updateWorkflowVersionModal #modal-refGenomes-version').select(allowedRefGen, { force: true });
        cy.checkedTyping(() => cy.get('@modal').find('textarea#comment'), comment);
        cy.get('@modal').find('input#deprecate-state').check({ force: true });

        cy.get('@modal').find('button#confirmModal').click();

        cy.wait('@updateWorkflowVersion').then((interception2) => {
          expect(interception2.response.statusCode).to.eq(200);
          cy.get(`#modify-btn-${workflowVersionId}`).parent().parent().as('wvRow');
          cy.get('@wvRow').contains(interception2.response.body.comment);
          cy.get('@wvRow').contains(interception2.response.body.allowedRefGenomes[0].name);
          cy.get('@wvRow').contains(interception2.response.body.supportedSeqTypes[0].displayName);
          cy.get('@wvRow').contains(interception2.response.body.commentData.date);
          cy.get('@wvRow').contains(interception2.response.body.commentData.author);
          cy.get('@wvRow').contains(interception2.response.body.deprecateDate);
        });

        // undeprecate workflow version
        cy.get(`#modify-btn-${workflowVersionId}`).click();
        cy.get('@modal').should('be.visible');

        cy.get('@modal').find('input#deprecate-state').uncheck({ force: true });
        cy.get('@modal').find('button#confirmModal').click();

        cy.wait('@updateWorkflowVersion').then((interception2) => {
          expect(interception2.response.statusCode).to.eq(200);
          cy.get('@wvRow').contains(interception2.response.body.name);
          cy.get('@wvRow').find('td').eq(1).should('have.text', supportedSeqType);
          cy.get('@wvRow').find('td').eq(2).should('have.text', allowedRefGen);
          cy.get('@wvRow').find('td').eq(3).should('have.text', comment);
        });
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowSystemConfig/index');
      cy.checkAccessDenied('/workflowSystemConfig/updateWorkflow');
      cy.checkAccessDenied('/workflowSystemConfig/getWorkflowVersions');
      cy.checkAccessDenied('/workflowSystemConfig/updateWorkflowVersion');
    });
  });
});
