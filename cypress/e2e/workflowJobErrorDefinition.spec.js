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

describe('Check workflow job error definition page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should create a workflow job error definition', () => {
      cy.visit('/workflowJobErrorDefinition/create');
      cy.intercept('workflowJobErrorDefinition/createObject*').as('createErrorDefinition');

      cy.fixture('workflowJobErrorDefinition.json').then((errorDefinition) => {
        cy.get('input#name').type(errorDefinition[0].name);
        cy.get('select#jobBeanName').select(errorDefinition[0].jobBeanName, { force: true });
        cy.get('select#sourceType').select(errorDefinition[0].sourceType, { force: true });
        cy.get('select#restartAction').select(errorDefinition[0].restartAction, { force: true });
        cy.get('select#beanToRestart').select(errorDefinition[0].beanToRestart, { force: true });
        cy.get('input#errorExpression').type(errorDefinition[0].errorExpression);
        cy.get('input#allowRestartingCount').type(errorDefinition[0].allowRestartingCount);
        cy.get('textarea#mailText').type(errorDefinition[0].mailText);
      });

      cy.get('input#create').click();

      cy.wait('@createErrorDefinition').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowJobErrorDefinition/index');
      });
    });

    it('should edit a workflow job error definition', () => {
      cy.visit('/workflowJobErrorDefinition/index');
      cy.intercept('workflowJobErrorDefinition/updateField*').as('updateErrorDefinition');
      cy.intercept('workflowJobErrorDefinition/updateActionField*').as('updateActionFieldForErrorDefinition');

      cy.fixture('workflowJobErrorDefinition.json').then((errorDefinition) => {
        cy.get('div.errorDefinitions tbody tr').eq(1).find('td').then((errorColumns) => {
          cy.wrap(errorColumns).eq(0).find('button.edit').click();
          cy.wrap(errorColumns).eq(0).find('input.edit-switch-input').clear()
            .type(errorDefinition[1].name);
          cy.wrap(errorColumns).eq(0).find('button.save').click();
          cy.wait('@updateErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });

          cy.wrap(errorColumns).eq(1).find('button.edit').click();
          cy.wrap(errorColumns).eq(1).find('select').select(errorDefinition[1].jobBeanName, { force: true });
          cy.wrap(errorColumns).eq(1).find('button.save').click();
          cy.wait('@updateErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });

          cy.wrap(errorColumns).eq(2).find('button.edit').click();
          cy.wrap(errorColumns).eq(2).find('select').select(errorDefinition[1].sourceType, { force: true });
          cy.wrap(errorColumns).eq(2).find('button.save').click();
          cy.wait('@updateErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });

          cy.wrap(errorColumns).eq(4).find('button.edit').click();
          cy.wrap(errorColumns).eq(4).find('select').select(errorDefinition[1].beanToRestart, { force: true });
          cy.wrap(errorColumns).eq(4).find('button.save').click();
          cy.wait('@updateErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });

          cy.wrap(errorColumns).eq(3).find('button.edit').click();
          cy.wrap(errorColumns).eq(3).find('select').select(errorDefinition[1].restartAction, { force: true });
          cy.wrap(errorColumns).eq(3).find('button.save').click();
          cy.wait('@updateActionFieldForErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });

          cy.wrap(errorColumns).eq(5).find('button.edit').click();
          cy.wrap(errorColumns).eq(5).find('input.edit-switch-input').type(errorDefinition[1].errorExpression);
          cy.wrap(errorColumns).eq(5).find('button.save').click();
          cy.wait('@updateErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });

          cy.wrap(errorColumns).eq(6).find('button.edit').click();
          cy.wrap(errorColumns).eq(6).find('input.edit-switch-input').type(errorDefinition[1].allowRestartingCount);
          cy.wrap(errorColumns).eq(6).find('button.save').click();
          cy.wait('@updateErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });

          cy.wrap(errorColumns).eq(7).find('button.edit-button-left').click();
          cy.wrap(errorColumns).eq(7).find('textarea').type(errorDefinition[1].mailText);
          cy.wrap(errorColumns).eq(7).find('button.save').click();
          cy.wait('@updateErrorDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            expect(interception.response.body.success).to.eq(true);
          });
        });
      });
    });

    it('should delete the workflow job error definition', () => {
      cy.visit('/workflowJobErrorDefinition/index');
      cy.intercept('/workflowJobErrorDefinition/delete*').as('deleteWorkflowJobErrorDefinition');

      cy.get('div.errorDefinitions tbody tr').eq(1).find('td.delete input#delete').click();
      cy.wait('@deleteWorkflowJobErrorDefinition').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.get('div.errorDefinitions').find('tbody').should('have.length', 1);
      });
    });
  });
});
