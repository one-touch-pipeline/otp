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

describe('Check workflow run overview page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
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
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowRunOverview/index');
    });
  });
});
