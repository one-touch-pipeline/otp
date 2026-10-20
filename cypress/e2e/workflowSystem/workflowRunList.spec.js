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

describe('Check workflow run list page', () => {
  'use strict';

  context('when user is an operator selecting runs to kill', () => {
    const nonexistentRunId = '999999999';
    // state groups offered by the state filter
    const inputRequired = 'WAITING_FOR_USER,FAILED,FAILED_WAITING';
    const notFinished = 'PENDING,RUNNING_WES,RUNNING_OTP';

    // each test starts from the same data, since killing a run cannot be undone by the application
    beforeEach(() => {
      cy.beginTest();
      cy.loginAs('operator');
      cy.intercept('/workflowRunList/data*').as('workflowRunListData');
    });

    const visitList = (state) => {
      cy.visit(`/workflowRunList/index?state=${state}`);
      cy.wait('@workflowRunListData').its('response.statusCode').should('eq', 200);
      cy.get('table#runs tbody input.tableCheckbox').should('exist');
    };

    const switchToKillMode = () => {
      cy.get('label[for="selectionModeKill"]').click();
      cy.get('#selectionModeKill').should('be.checked');
    };

    it('should default to restart mode, selecting only failed runs by step ID', () => {
      visitList(inputRequired);

      cy.get('#selectionModeRestart').should('be.checked');
      cy.get('#killRunsButton').should('be.disabled');
      cy.get('#bulk .restart-mode-btn').should('be.enabled');
      cy.get('table#runs tbody input.tableCheckbox[data-state="FAILED"]').should('exist');
      cy.get('table#runs tbody input.tableCheckbox:not([disabled])').each((checkbox) => {
        expect(checkbox).to.have.attr('name', 'step');
        expect(checkbox).to.have.attr('data-state', 'FAILED');
        expect(checkbox.val()).to.eq(checkbox.attr('data-step-id'));
      });
      cy.get('table#runs tbody input.tableCheckbox[data-state="WAITING_FOR_USER"]').should('be.disabled');

      cy.get('select#state').select(notFinished, { force: true });
      cy.wait('@workflowRunListData').its('response.statusCode').should('eq', 200);
      cy.get('table#runs tbody input.tableCheckbox').should('exist');
      cy.get('table#runs tbody input.tableCheckbox:not([disabled])').should('not.exist');
    });

    it('should select only unfinished runs by run ID in kill mode and clear the selection on mode changes', () => {
      visitList(inputRequired);
      cy.get('#selectAll').click();
      cy.get('#selectedCount').invoke('text').then(parseInt).should('be.gt', 0);

      switchToKillMode();

      cy.get('#selectedCount').should('have.text', '0');
      cy.get('#selectAll').should('not.be.checked');
      cy.get('table#runs tbody input.tableCheckbox:checked').should('not.exist');
      cy.get('#killRunsButton').should('be.enabled');
      cy.get('#bulk .restart-mode-btn').should('be.disabled');
      cy.get('table#runs tbody input.tableCheckbox[data-state="FAILED"]').should('be.disabled');

      cy.get('select#state').select(notFinished, { force: true });
      cy.wait('@workflowRunListData').its('response.statusCode').should('eq', 200);
      cy.get('#selectionModeKill').should('be.checked');
      cy.get('table#runs tbody input.tableCheckbox:not([disabled])').each((checkbox) => {
        expect(checkbox).to.have.attr('name', 'run');
        expect(['PENDING', 'RUNNING_OTP', 'RUNNING_WES']).to.include(checkbox.attr('data-state'));
        expect(checkbox.val()).to.eq(checkbox.attr('data-run-id'));
      });
      // a PENDING run has no step, but can be selected for killing
      cy.get('table#runs tbody input.tableCheckbox[data-state="PENDING"][data-step-id=""]').first().should('be.enabled');

      cy.get('#selectAll').click();
      cy.get('table#runs tbody input.tableCheckbox:not([disabled])').its('length').then((eligible) => {
        cy.get('#selectedCount').should('have.text', `${eligible}`);
      });

      cy.get('label[for="selectionModeRestart"]').click();
      cy.get('#selectedCount').should('have.text', '0');
      cy.get('table#runs tbody input.tableCheckbox:checked').should('not.exist');
      cy.get('#killRunsButton').should('be.disabled');
    });

    it('should select only the displayed page and clear the selection when paging or filtering', () => {
      // all runs of this group can be killed, and there are more than fit on one page of 10
      visitList(notFinished);
      cy.get('select[name="runs_length"]').select('10', { force: true });
      cy.wait('@workflowRunListData').its('response.statusCode').should('eq', 200);
      cy.get('.paginate_button.next').should('not.have.class', 'disabled');
      switchToKillMode();

      cy.get('#selectAll').click();
      cy.get('table#runs tbody input.tableCheckbox').should('have.length', 10);
      cy.get('#selectedCount').should('have.text', '10');

      cy.get('.paginate_button.next').click();
      cy.wait('@workflowRunListData').its('response.statusCode').should('eq', 200);
      cy.get('#selectedCount').should('have.text', '0');
      cy.get('#selectAll').should('not.be.checked');
      cy.get('table#runs tbody input.tableCheckbox:checked').should('not.exist');

      cy.get('table#runs tbody input.tableCheckbox').first().check();
      cy.get('#selectedCount').should('have.text', '1');
      cy.get('select#state').select('PENDING', { force: true });
      cy.wait('@workflowRunListData').its('response.statusCode').should('eq', 200);
      cy.get('#selectedCount').should('have.text', '0');
      cy.get('table#runs tbody input.tableCheckbox:checked').should('not.exist');
    });

    it('should not send a kill request for an empty selection or a cancelled confirmation', () => {
      let killRequests = 0;
      cy.intercept('POST', '/workflowRunList/killRuns*', () => {
        killRequests += 1;
      });
      visitList('PENDING');
      switchToKillMode();

      const alertStub = cy.stub().as('alert');
      cy.on('window:alert', alertStub);
      cy.get('#killRunsButton').click();
      cy.get('@alert').should('have.been.calledWith', 'Select at least one row');

      const confirmStub = cy.stub().returns(false).as('confirm');
      cy.on('window:confirm', confirmStub);
      cy.get('table#runs tbody input.tableCheckbox').eq(0).check();
      cy.get('table#runs tbody input.tableCheckbox').eq(1).check();
      cy.get('#killRunsButton').click();
      cy.get('@confirm').should('have.been.calledWith', 'Kill 2 selected workflow run(s)?');

      cy.location('pathname').should('eq', '/workflowRunList/index');
      cy.get('table#runs tbody input.tableCheckbox:checked').should('have.length', 2);
      cy.then(() => expect(killRequests).to.eq(0));
    });

    it('should kill the selected runs after confirmation and report the success', () => {
      cy.intercept('POST', '/workflowRunList/killRuns*').as('killRuns');
      visitList('PENDING');
      switchToKillMode();

      const confirmStub = cy.stub().returns(true).as('confirm');
      cy.on('window:confirm', confirmStub);
      cy.get('table#runs tbody input.tableCheckbox').eq(0).check();
      cy.get('table#runs tbody input.tableCheckbox').eq(1).check();
      cy.get('table#runs tbody input.tableCheckbox:checked').then((checked) => {
        const runIds = checked.toArray().map((el) => el.value);
        cy.get('#killRunsButton').click();
        cy.get('@confirm').should('have.been.calledWith', 'Kill 2 selected workflow run(s)?');

        cy.wait('@killRuns').then((interception) => {
          const body = new URLSearchParams(interception.request.body);
          expect(body.getAll('run')).to.deep.eq(runIds);
          expect(body.getAll('step')).to.be.empty;
          expect(interception.response.statusCode).to.eq(302);
        });
        cy.location('pathname').should('contain', '/workflowRunList');
        cy.get('.toast.otpSuccessToast .toast-body').should('contain.text', 'Killed 2 runs successfully');
        cy.get('.toast-body li').should('not.exist');

        runIds.forEach((runId) => {
          cy.visit(`/workflowRunDetails/index/${runId}`);
          cy.get('#statusDot').should('have.attr', 'data-status', 'KILLED');
        });
      });
    });

    it('should report a partial success with the failed run ID and reason', () => {
      visitList('PENDING');
      switchToKillMode();

      cy.get('table#runs tbody input.tableCheckbox').eq(0).check();
      // an ID of a run that does not exist (anymore) is rejected by the server for that run only
      cy.get('table#runs tbody input.tableCheckbox').eq(1).invoke('val', nonexistentRunId).check();
      cy.get('table#runs tbody input.tableCheckbox').eq(0).invoke('val').then((killedRunId) => {
        cy.get('#killRunsButton').click();

        cy.get('.toast.otpErrorToast .toast-body').should('contain.text', 'Killed 1 of 2 runs');
        cy.get('.toast-body li').should('have.length', 1)
          .and('have.text', `${nonexistentRunId}: Run does not exist`);

        cy.visit(`/workflowRunDetails/index/${killedRunId}`);
        cy.get('#statusDot').should('have.attr', 'data-status', 'KILLED');
      });
    });

    it('should report when no selected run could be killed', () => {
      visitList('PENDING');
      switchToKillMode();

      cy.get('table#runs tbody input.tableCheckbox').eq(0).invoke('val', nonexistentRunId).check();
      cy.get('#killRunsButton').click();

      cy.get('.toast.otpErrorToast .toast-body').should('contain.text', 'Failed to kill runs');
      cy.get('.toast-body li').should('have.length', 1)
        .and('have.text', `${nonexistentRunId}: Run does not exist`);
    });

    it('should kill a single run from its row and keep the restart buttons of failed runs', () => {
      cy.intercept('POST', /\/workflowRunList\/killRun(\?.*)?$/).as('killRun');
      visitList(inputRequired);

      cy.get('table#runs tbody input.tableCheckbox[data-state="FAILED"]').first().closest('tr')
        .within(() => {
          cy.get('button.kill-run-btn').should('be.disabled');
          cy.get('button.restart-run-btn').should('be.enabled');
        });

      visitList('PENDING');
      cy.get('table#runs tbody input.tableCheckbox[data-state="PENDING"]').first().invoke('attr', 'data-run-id')
        .then((runId) => {
          cy.get(`table#runs tbody button.kill-run-btn[value="${runId}"]`).should('be.enabled').click();

          cy.wait('@killRun').then((interception) => {
            expect(new URLSearchParams(interception.request.body).get('run')).to.eq(runId);
            expect(interception.response.statusCode).to.eq(302);
          });
          cy.get('.toast-body').should('contain.text', 'Killed runs successfully');
          cy.visit(`/workflowRunDetails/index/${runId}`);
          cy.get('#statusDot').should('have.attr', 'data-status', 'KILLED');
        });
    });
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/workflowRunList/index?state=FAILED');
    });

    it('should show "First job started" and "Last job finished" columns', () => {
      cy.intercept('/workflowRunList/data*').as('workflowRunListData');

      cy.get('select#state').select('WAITING_FOR_USER', { force: true });
      cy.wait('@workflowRunListData').its('response.statusCode').should('eq', 200);
      cy.get('table#runs tbody').should('not.be.empty');

      cy.get('table#runs thead th').contains('First job started');
      cy.get('table#runs thead th').contains('Last job finished');

    });

    it('should filter the list by state', () => {
      cy.intercept('/workflowRunList/data*').as('workflowRunListFilter');

      const successClass = 'green';
      const failedClass = 'red';

      cy.get('table#runs .dot').should('have.class', failedClass);
      cy.get('table#runs .dot').should('not.have.class', successClass);

      cy.get('select#state').select('SUCCESS', { force: true });

      cy.wait('@workflowRunListFilter').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('table#runs .dot').should('have.class', successClass);
        cy.get('table#runs .dot').should('not.have.class', failedClass);
      });
    });

    it('should set a failed workflow to failed final', () => {
      cy.get('td button.failed-final-btn').first().click();
      cy.get('.toast-body').should('contain.text', 'Success');
    });

    it('should restart a failed workflow step', () => {
      cy.get('td button.restart-step-btn').last().click();
      cy.get('.toast-body').should('contain.text', 'success');
    });

    it('should restart the previous step of a failed workflow', () => {
      cy.get('button.dropdown-toggle').last().click();
      cy.get('button.dropdown-item').last().click();
      cy.get('.toast-body').should('contain.text', 'success');
    });

    it('should restart a failed workflow run', () => {
      cy.get('td button.restart-run-btn').first().click();
      cy.get('.toast-body').should('contain.text', 'success');
    });

    it('should accept POST requests to kill runs and reject GET requests', () => {
      cy.request({
        method: 'POST',
        url: '/workflowRunList/killRuns',
        form: true,
        body: { run: 'invalid' },
        followRedirect: false,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(302);
        expect(response.headers.location).to.include('/workflowRunList/index');
      });

      cy.request({
        method: 'GET',
        url: '/workflowRunList/killRuns',
        failOnStatusCode: false
      }).its('status').should('eq', 405);
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowRunList/index');
      cy.checkAccessDenied('/workflowRunList/data');
    });

    it('should reject POST requests to kill runs', () => {
      // a denied POST is forwarded to the GET-only ErrorController#error403 and therefore answered with 405,
      // the same as for the other POST-only operator actions (e.g. restartRun, killRun)
      cy.request({
        method: 'POST',
        url: '/workflowRunList/killRuns',
        form: true,
        body: { run: '1' },
        failOnStatusCode: false
      }).its('status').should('eq', 405);
    });
  });
});
