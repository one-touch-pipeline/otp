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

describe('Check workflow defaults', {
  retries: 1
}, () => {
  'use strict';

  const toastSuccess = '#otpToastBox .otpSuccessToast';
  const toastError = '#otpToastBox .otpErrorToast';

  // Clicking a toast's close button only starts its Bootstrap fade-out transition, it doesn't remove
  // it from the DOM immediately - so the next assertion waits for it to actually be gone rather than
  // just firing the click, otherwise a still-fading toast can block a click on whatever's underneath it
  // (the toast box is fixed top-right, overlapping the table's rightmost Edit column).
  const checkAndDismissToast = (selector, expectedText) => {
    cy.get(selector).last().should('be.visible').and('contain.text', expectedText)
      .find('button.close').click();
    cy.get(selector).should('not.exist');
  };

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should create a default, apply it to a project, then clean up both', () => {
      cy.fixture('workflowSelection.json').then((config) => {
        const groupName = `Cypress Default ${Date.now()}`;
        const analysisConfig = config.analysis[0];

        // create the default
        cy.intercept('/workflowSystemConfig/getWorkflowDefaultGroups*').as('getDefaults');
        cy.intercept('/workflowSystemConfig/getWorkflowVersions*').as('getWorkflowVersions');
        cy.intercept('/workflowSystemConfig/saveWorkflowDefaultGroup*').as('saveDefault');

        cy.visit('/workflowSystemConfig/index');
        cy.wait('@getDefaults');

        cy.get('#add-workflow-default-btn').click();
        cy.get('#workflowDefaultModal').should('be.visible');

        cy.checkedTyping(() => cy.get('#default-modal-name'), groupName);
        cy.get('#default-modal-seqType').select(analysisConfig.seqType, { force: true });
        cy.get('.default-entry-workflow-select').select(analysisConfig.workflow, { force: true });
        cy.wait('@getWorkflowVersions');
        cy.get('.default-entry-version-select').select(analysisConfig.version.id, { force: true });

        cy.get('#workflowDefaultModal #confirmModal').click();

        cy.wait('@saveDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        checkAndDismissToast(toastSuccess, 'Workflow default saved.');
        cy.wait('@getDefaults');

        cy.contains('#analysisDefaultsTable td', groupName).as('nameCell');
        cy.get('@nameCell').siblings().should('contain.text', analysisConfig.workflow);
        cy.get('@nameCell').siblings().should('contain.text', analysisConfig.seqType);
        cy.get('@nameCell').siblings().should('contain.text', analysisConfig.version.name);

        // apply the default
        cy.visit('/workflowSelection/index');
        cy.get('h2#headingAnalysis').click();

        cy.intercept('/workflowSelection/searchWorkflowDefaultGroups*').as('searchDefaults');
        cy.intercept('/workflowSelection/applyWorkflowDefaultGroup*').as('applyDefault');
        cy.intercept('/workflowSelection/deleteConfiguration*').as('deleteConfig');

        cy.get('#select2-analysis-default-select-container').click();
        cy.get('.select2-container--open .select2-search__field').type(groupName);
        cy.wait('@searchDefaults');
        cy.get('li.select2-results__option').contains(groupName).click({ force: true });

        cy.get('#add-analysis-default-btn').click();
        cy.wait('@applyDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.have.length(1);

          const wvSelectorId = interception.response.body[0].workflowVersionSelector.id;
          const btnSelector = `button[data-version-selector=${wvSelectorId}]`;
          cy.get(`table#analysisTable ${btnSelector}`).as('removeBtn');
          cy.get('@removeBtn').parent().parent().find('td').as('createdCells');

          cy.get('@createdCells').contains(analysisConfig.workflow);
          cy.get('@createdCells').contains(analysisConfig.seqType);
          cy.get('@createdCells').contains(analysisConfig.version.name);
        });
        checkAndDismissToast(toastSuccess, 'Default workflows added.');

        // clean up
        cy.get('@removeBtn').click();
        cy.wait('@deleteConfig').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          cy.get('@removeBtn').should('not.exist');
        });
        checkAndDismissToast(toastSuccess, 'Successfully deleted analysis configuration');

        cy.intercept('/workflowSystemConfig/getWorkflowDefaultGroups*').as('getDefaultsAgain');
        cy.intercept('/workflowSystemConfig/deleteWorkflowDefaultGroup*').as('deleteDefault');
        cy.visit('/workflowSystemConfig/index');
        cy.wait('@getDefaultsAgain');

        cy.contains('#analysisDefaultsTable td', groupName).siblings()
          .find('.workflow-default-edit-btn').click();
        cy.get('#workflowDefaultModal #default-modal-delete-btn').click();

        cy.wait('@deleteDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        checkAndDismissToast(toastSuccess, 'Workflow default deleted.');
        cy.contains('#analysisDefaultsTable td', groupName).should('not.exist');
      });
    });

    it('should edit an existing default, renaming it and reassigning its workflow version', () => {
      cy.fixture('workflowSelection.json').then((config) => {
        const groupName = `Cypress Editable Default ${Date.now()}`;
        const renamedGroupName = `${groupName} (edited)`;
        const initialAnalysisConfig = config.analysis[0];
        const editedAnalysisConfig = config.analysis[1];

        cy.intercept('/workflowSystemConfig/getWorkflowDefaultGroups*').as('getDefaults');
        cy.intercept('/workflowSystemConfig/getWorkflowVersions*').as('getWorkflowVersions');
        cy.intercept('/workflowSystemConfig/saveWorkflowDefaultGroup*').as('saveDefault');
        cy.intercept('/workflowSystemConfig/deleteWorkflowDefaultGroup*').as('deleteDefault');

        cy.visit('/workflowSystemConfig/index');
        cy.wait('@getDefaults');

        cy.get('#add-workflow-default-btn').click();
        cy.checkedTyping(() => cy.get('#default-modal-name'), groupName);
        cy.get('#default-modal-seqType').select(initialAnalysisConfig.seqType, { force: true });
        cy.get('.default-entry-workflow-select').select(initialAnalysisConfig.workflow, { force: true });
        cy.wait('@getWorkflowVersions');
        cy.get('.default-entry-version-select').select(initialAnalysisConfig.version.id, { force: true });
        cy.get('#workflowDefaultModal #confirmModal').click();
        cy.wait('@saveDefault');
        checkAndDismissToast(toastSuccess, 'Workflow default saved.');
        cy.wait('@getDefaults');

        // edit
        cy.contains('#analysisDefaultsTable td', groupName).siblings()
          .find('.workflow-default-edit-btn').click();
        // opening the modal itself triggers a getWorkflowVersions call to populate the existing entry row -
        // consume that here so the later wait (after selecting the new workflow) doesn't pick up this one instead
        cy.wait('@getWorkflowVersions');
        cy.get('#workflowDefaultModal').should('be.visible');
        cy.get('#workflowDefaultModal .modal-title').should('contain.text', 'Edit');

        cy.checkedTyping(() => cy.get('#default-modal-name'), renamedGroupName);
        cy.get('.default-entry-workflow-select').select(editedAnalysisConfig.workflow, { force: true });
        cy.wait('@getWorkflowVersions');
        cy.get('.default-entry-version-select').select(editedAnalysisConfig.version.id, { force: true });
        cy.get('#workflowDefaultModal #confirmModal').click();

        cy.wait('@saveDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        checkAndDismissToast(toastSuccess, 'Workflow default saved.');
        cy.wait('@getDefaults');

        cy.contains('#analysisDefaultsTable td', new RegExp(`^${groupName}$`)).should('not.exist');
        cy.contains('#analysisDefaultsTable td', renamedGroupName).as('nameCell');
        cy.get('@nameCell').siblings().should('contain.text', editedAnalysisConfig.workflow);
        cy.get('@nameCell').siblings().should('contain.text', editedAnalysisConfig.version.name);

        // clean up
        cy.get('@nameCell').siblings().find('.workflow-default-edit-btn').click();
        cy.get('#workflowDefaultModal #default-modal-delete-btn').click();
        cy.wait('@deleteDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        checkAndDismissToast(toastSuccess, 'Workflow default deleted.');
      });
    });

    it('should show an error toast when saving a default with a blank name', () => {
      cy.fixture('workflowSelection.json').then((config) => {
        const groupName = `Cypress Invalid Default ${Date.now()}`;
        const analysisConfig = config.analysis[0];

        cy.intercept('/workflowSystemConfig/getWorkflowDefaultGroups*').as('getDefaults');
        cy.intercept('/workflowSystemConfig/getWorkflowVersions*').as('getWorkflowVersions');
        cy.intercept('/workflowSystemConfig/saveWorkflowDefaultGroup*').as('saveDefault');
        cy.intercept('/workflowSystemConfig/deleteWorkflowDefaultGroup*').as('deleteDefault');

        cy.visit('/workflowSystemConfig/index');
        cy.wait('@getDefaults');

        cy.get('#add-workflow-default-btn').click();
        cy.checkedTyping(() => cy.get('#default-modal-name'), groupName);
        cy.get('#default-modal-seqType').select(analysisConfig.seqType, { force: true });
        cy.get('.default-entry-workflow-select').select(analysisConfig.workflow, { force: true });
        cy.wait('@getWorkflowVersions');
        cy.get('.default-entry-version-select').select(analysisConfig.version.id, { force: true });
        cy.get('#workflowDefaultModal #confirmModal').click();
        cy.wait('@saveDefault');
        checkAndDismissToast(toastSuccess, 'Workflow default saved.');
        cy.wait('@getDefaults');

        // attempt an invalid save (blank name)
        cy.contains('#analysisDefaultsTable td', groupName).siblings()
          .find('.workflow-default-edit-btn').click();
        // opening the modal itself triggers a getWorkflowVersions call to populate the existing entry row
        cy.wait('@getWorkflowVersions');
        cy.get('#workflowDefaultModal').should('be.visible');

        cy.get('#default-modal-name').clear();
        cy.get('#workflowDefaultModal #confirmModal').click();

        cy.wait('@saveDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(406);
        });
        checkAndDismissToast(toastError, 'Failed to save default.');

        cy.contains('#analysisDefaultsTable td', groupName).should('exist');

        // clean up
        cy.contains('#analysisDefaultsTable td', groupName).siblings()
          .find('.workflow-default-edit-btn').click();
        cy.get('#workflowDefaultModal #default-modal-delete-btn').click();
        cy.wait('@deleteDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        checkAndDismissToast(toastSuccess, 'Workflow default deleted.');
      });
    });

    it('should create a default with multiple entries and correctly handle removing an entry', () => {
      cy.fixture('workflowSelection.json').then((config) => {
        const groupName = `Cypress Multi Entry Default ${Date.now()}`;
        const firstEntry = config.analysis[0];
        const secondEntry = config.analysis[1];

        cy.intercept('/workflowSystemConfig/getWorkflowDefaultGroups*').as('getDefaults');
        cy.intercept('/workflowSystemConfig/getWorkflowVersions*').as('getWorkflowVersions');
        cy.intercept('/workflowSystemConfig/saveWorkflowDefaultGroup*').as('saveDefault');
        cy.intercept('/workflowSystemConfig/deleteWorkflowDefaultGroup*').as('deleteDefault');

        cy.visit('/workflowSystemConfig/index');
        cy.wait('@getDefaults');

        cy.get('#add-workflow-default-btn').click();
        cy.get('#workflowDefaultModal').should('be.visible');

        cy.get('.default-modal-entry-row').should('have.length', 1);
        cy.get('.default-modal-entry-row').eq(0).find('button').click();
        cy.get('.default-modal-entry-row').should('have.length', 1);

        cy.checkedTyping(() => cy.get('#default-modal-name'), groupName);
        cy.get('#default-modal-seqType').select(firstEntry.seqType, { force: true });

        cy.get('.default-modal-entry-row').eq(0).find('.default-entry-workflow-select')
          .select(firstEntry.workflow, { force: true });
        cy.wait('@getWorkflowVersions');
        cy.get('.default-modal-entry-row').eq(0).find('.default-entry-version-select')
          .select(firstEntry.version.id, { force: true });

        // add a second entry
        cy.get('#default-modal-add-row-btn').click();
        cy.get('.default-modal-entry-row').should('have.length', 2);
        cy.get('.default-modal-entry-row').eq(1).find('.default-entry-workflow-select')
          .select(secondEntry.workflow, { force: true });
        cy.wait('@getWorkflowVersions');
        cy.get('.default-modal-entry-row').eq(1).find('.default-entry-version-select')
          .select(secondEntry.version.id, { force: true });

        // add a third, throwaway entry, then remove it again via its remove button
        cy.get('#default-modal-add-row-btn').click();
        cy.get('.default-modal-entry-row').should('have.length', 3);
        cy.get('.default-modal-entry-row').eq(2).find('button').click();
        cy.get('.default-modal-entry-row').should('have.length', 2);

        cy.get('#workflowDefaultModal #confirmModal').click();
        cy.wait('@saveDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        checkAndDismissToast(toastSuccess, 'Workflow default saved.');
        cy.wait('@getDefaults');

        // reopen and verify exactly the two intended entries were saved, not the removed throwaway one
        cy.contains('#analysisDefaultsTable td', groupName).siblings()
          .find('.workflow-default-edit-btn').click();
        cy.get('#workflowDefaultModal').should('be.visible');
        cy.get('.default-modal-entry-row').should('have.length', 2);
        cy.get('.default-modal-entry-row').eq(0).find('.default-entry-workflow-select option:selected')
          .should('contain.text', firstEntry.workflow);
        cy.get('.default-modal-entry-row').eq(1).find('.default-entry-workflow-select option:selected')
          .should('contain.text', secondEntry.workflow);

        // clean up
        cy.get('#workflowDefaultModal #default-modal-delete-btn').click();
        cy.wait('@deleteDefault').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        checkAndDismissToast(toastSuccess, 'Workflow default deleted.');
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to manage workflow defaults', () => {
      cy.checkAccessDenied('/workflowSystemConfig/getWorkflowDefaultGroups');
      cy.checkAccessDenied('/workflowSystemConfig/saveWorkflowDefaultGroup');
      cy.checkAccessDenied('/workflowSystemConfig/deleteWorkflowDefaultGroup');
    });

    it('should not see the "select a default" controls on the workflow selection page', () => {
      cy.visit('/workflowSelection/index');
      cy.get('h2#headingAnalysis').click();

      cy.get('#analysis-default-select').should('not.exist');
      cy.get('#add-analysis-default-btn').should('not.exist');
    });
  });
});
