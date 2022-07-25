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

describe('Check workflow system configuration page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
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

      cy.get('#editWorkflowModal').find('.modal-title').should('contain.text', 'Cell Ranger');
      cy.get('#editWorkflowModal #modal-priority').clear().type(priority);
      cy.get('#editWorkflowModal #modal-max-runs').clear().type(maxRuns);
      cy.get('#editWorkflowModal #modal-seqTypes').select(supportedSeqType, { force: true });

      cy.get('#editWorkflowModal #confirmModal').click();

      cy.wait('@updateWorkflow').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('td').contains('Cell Ranger').siblings().contains(priority);
        cy.get('td').contains('Cell Ranger').siblings().contains(maxRuns);
        cy.get('td').contains('Cell Ranger').siblings().contains(supportedSeqType);
      });
    });
  });
});