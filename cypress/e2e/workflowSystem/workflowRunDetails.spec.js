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

describe('Check workflow run details page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should visit the error log of a restarted workflow run', () => {
      cy.intercept('/workflowRunDetails/data*').as('workflowRunDetailsData');
      cy.intercept('/workflowRunDetails/showError/*').as('showWorkflowErrors');
      cy.visit('workflowRunList/index?state=RESTARTED');

      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs tbody tr').first().find('a').click();

      cy.wait('@workflowRunDetailsData').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
      });

      cy.get('table#steps').find('tr.error-row').find('a').click();

      cy.wait('@showWorkflowErrors').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.location('pathname').should('contain', '/workflowRunDetails/showError');
      });
    });

    it('should visit the logs of a successfully workflow run', () => {
      cy.intercept('/workflowRunList/data*').as('data');
      cy.intercept('/workflowRunDetails/showLogs/*').as('showWorkflowLogs');

      cy.visit('/workflowRunList/index?state=SUCCESS');
      cy.wait('@data').its('response.statusCode').should('eq', 200);

      cy.get('input#name').type('example_2{enter}');
      cy.wait('@data').its('response.statusCode').should('eq', 200);

      cy.get('table#runs tbody').should('not.be.empty');
      cy.get('table#runs tbody tr').first().find('a').click();

      cy.get('table#steps button.details-control').first().click();
      cy.get('table#steps tr.log-row').first().find('a').click();

      cy.wait('@showWorkflowLogs').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.location('pathname').should('contain', '/workflowRunDetails/showLogs');
      });
    });

    it('should save a comment for a failed final workflow run', () => {
      cy.intercept('/workflowRunDetails/saveComment*').as('saveWorkflowRunComment');
      cy.visit('/workflowRunList/index?state=FAILED_FINAL');

      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs a').first().click();
      cy.location('pathname').should('contain', '/workflowRunDetails/index');

      cy.get('textarea#comment-content').type('Some test comment');
      cy.get('div#comment-box-container button#button-save').click();

      cy.wait('@saveWorkflowRunComment').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
    });

    it('should visit a failed workflow and restart a step of it', () => {
      cy.visit('/workflowRunList/index?state=FAILED');

      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs tbody tr').last().find('a').click();
      cy.location('pathname').should('contain', '/workflowRunDetails/index');

      // Restart step
      cy.intercept('/workflowRunDetails/restartStep*').as('restartWorkflowStep');

      cy.get('form.restart-step-form button').not('disabled').first().click();
      cy.wait('@restartWorkflowStep').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
        cy.get('table#steps tbody').should('not.be.empty');
        cy.get('.otpSuccessToast').should('exist');
      });
    });

    it('should visit a failed workflow and restart it', () => {
      cy.visit('/workflowRunList/index?state=FAILED');

      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs tbody tr').last().find('a').click();
      cy.location('pathname').should('contain', '/workflowRunDetails/index');

      // Restart run
      cy.intercept('/workflowRunDetails/restartRun*').as('restartWorkflowRun');

      cy.get('form button.restart-run-btn').click();
      cy.wait('@restartWorkflowRun').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
        cy.get('div#statusDot').should('have.attr', 'data-status', 'RESTARTED');
      });
    });

    it('should visit a failed workflow and set it as failed final', () => {
      cy.visit('/workflowRunList/index?state=FAILED');

      cy.intercept('/workflowRunDetails/setFailedFinal*').as('setFailedFinal');
      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs tbody tr').first().find('a').click();
      cy.location('pathname').should('contain', '/workflowRunDetails/index');

      cy.get('form button.failed-final-btn').click();

      cy.wait('@setFailedFinal').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
        cy.get('#statusDot').should('have.attr', 'data-status', 'FAILED_FINAL');
        cy.get('form button.failed-waiting-btn').should('not.be.visible').and('not.be.enabled');
      });
    });

    it('should visit a failed workflow and set it as failed waiting', () => {
      cy.visit('/workflowRunList/index?state=FAILED');

      cy.intercept('/workflowRunDetails/toggleFailedWaiting*').as('toggleFailedWaiting');
      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs tbody tr').first().find('a').click();
      cy.location('pathname').should('contain', '/workflowRunDetails/index');
      cy.get('form button.failed-waiting-btn').should('be.visible', 'Set failed waiting');

      cy.get('form button.failed-waiting-btn').click();

      cy.wait('@toggleFailedWaiting').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
        cy.get('#statusDot').should('have.attr', 'data-status', 'FAILED_WAITING');
        cy.get('.toast-body').should('contain.text', 'Successfully set status to FAILED_WAITING');
        cy.get('form button.failed-waiting-btn').should('be.visible', 'Remove failed waiting');
      });
    });

    it('should visit a failed waiting waiting workflow and set it as failed final', () => {
      setWorkflowToFailedWaiting();
      cy.visit('/workflowRunList/index?state=FAILED_WAITING');

      cy.intercept('/workflowRunDetails/setFailedFinal*').as('setFailedFinal');
      cy.get('table#runs tbody').should('not.be.empty');
      cy.get('table#runs tbody tr').first().find('a').click();

      cy.location('pathname').should('contain', '/workflowRunDetails/index');
      cy.get('form button.failed-final-btn').should('be.enabled');

      cy.get('form button.failed-final-btn').click();

      cy.wait('@setFailedFinal').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
        cy.get('#statusDot').should('have.attr', 'data-status', 'FAILED_FINAL');
      });
    });

    it('should visit a failed waiting workflow and restart it', () => {
      setWorkflowToFailedWaiting();
      cy.visit('/workflowRunList/index?state=FAILED_WAITING');

      cy.get('table#runs tbody').should('not.be.empty');
      cy.get('table#runs tbody tr').last().find('a').click();

      cy.location('pathname').should('contain', '/workflowRunDetails/index');
      cy.get('form button.restart-run-btn').should('be.enabled');

      // Restart run
      cy.intercept('/workflowRunDetails/restartRun*').as('restartWorkflowRun');

      cy.get('form button.restart-run-btn').click();
      cy.wait('@restartWorkflowRun').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
        cy.get('div#statusDot').should('have.attr', 'data-status', 'RESTARTED');
      });

    });

    it('should visit a workflow on failed waiting and set it back to failed', () => {
      cy.visit('/workflowRunList/index?state=FAILED_WAITING');

      cy.intercept('/workflowRunDetails/toggleFailedWaiting*').as('toggleFailedWaiting');
      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs tbody tr').first().find('a').click();
      cy.location('pathname').should('contain', '/workflowRunDetails/index');
      cy.get('form button.failed-waiting-btn').should('be.visible', 'Remove failed waiting');

      cy.get('form button.failed-waiting-btn').click();

      cy.wait('@toggleFailedWaiting').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('contain', '/workflowRunDetails/index');
        cy.get('#statusDot').should('have.attr', 'data-status', 'FAILED');
        cy.get('.toast-body').should('contain.text', 'Successfully removed FAILED_WAITING status');
        cy.get('form button.failed-waiting-btn').should('be.visible', 'Set failed waiting');
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowRunList/index');
      cy.checkAccessDenied('/workflowRunDetails/data');
      cy.checkAccessDenied('/workflowRunDetails/showError');
      cy.checkAccessDenied('/workflowRunDetails/showLogs');
      cy.checkAccessDenied('/workflowRunDetails/saveComment');
      cy.checkAccessDenied('/workflowRunDetails/setFailedFinal');
      cy.checkAccessDenied('/workflowRunDetails/toggleFailedWaiting');
    });
  });
});


// This is a helper function to set a workflow to failed waiting since the test database currently does not have any failed waiting workflows
function setWorkflowToFailedWaiting() {
  cy.visit('/workflowRunList/index?state=FAILED');
  cy.get('table#runs tbody tr').first().find('a').click();
  cy.location('pathname').should('contain', '/workflowRunDetails/index');
  cy.get('form button.failed-waiting-btn').click();
}
