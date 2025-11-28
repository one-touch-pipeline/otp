/*
 * Copyright 2011-2025 The OTP authors
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

describe('Check workflow run overview page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should visit the index page', () => {
      cy.visit('/workflowRunOverview/index');
    });

    it('should see the list of workflow runs in the condensed view', () => {
      cy.visit('/workflowRunOverview/index');

      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs thead tr').find('th').should(ths => {
        expect(ths).to.contain('Input required');
        expect(ths).to.contain('Not finished');
        expect(ths).to.contain('Finished');
      });
    });

    it('should switch to detailed overview', function() {
      cy.visit('/workflowRunOverview/index');

      cy.get('div.container-fluid').find('button.toggleButton:visible')
        .contains('Show detailed states')
        .click();

      cy.get('.toggleButton:visible').should('have.text', 'Show condensed states');
      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs thead tr').last().find('th').should('have.length', 16);
      cy.fixture('workflowOverview').then((fixture) => {
        fixture.states.forEach((state) => {
          cy.get('table#runs thead tr').first().find('th').should('contain.text', state);
        });
        fixture.subStates.forEach((subState) => {
          cy.get('table#runs thead tr').last().find('th').should('contain.text', subState);
        });
      });
    });

    it('should open edit workflow modal when clicking a workflow status dot', () => {
      cy.intercept('GET', '**/workflowSystemConfig/getWorkflows*').as('getWorkflows');
      cy.intercept('POST', '**/workflowSystemConfig/updateWorkflow').as('updateWorkflow');

      cy.visit('/workflowRunOverview/index');
      cy.wait('@getWorkflows', { timeout: 15000 });

      cy.get('#editWorkflowModal').should('exist').and('not.be.visible');

      cy.get('table#runs tbody .dot.small[data-workflow-id]')
        .should('have.length.greaterThan', 0)
        .first()
        .click();

      cy.get('#editWorkflowModal', { timeout: 10000 })
        .should('have.class', 'show')
        .and('be.visible');

      cy.get('#modal-priority').should('exist');
      cy.get('#modal-max-runs').should('exist');
      cy.get('#modal-defaultVersion').should('exist');
      cy.get('#modal-seqTypes').should('exist');
      cy.get('#modal-refGenomes').should('exist');
      cy.get('#modal-enabled').should('exist');

      cy.get('#editWorkflowModal #confirmModal')
        .should('be.visible')
        .click();
    });

  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowRunOverview/index');
    });
  });
});
