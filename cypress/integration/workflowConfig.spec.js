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

describe('Check workflow config page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/workflowConfig');
    });

    it('should create a workflow configuration', () => {
      cy.intercept('/workflowConfig/create*').as('createWorkflowConfig');

      cy.get('#create-button').click();

      cy.fixture('workflowConfig.json').then((config) => {
        cy.get('div.modal-content').should('be.visible');

        // Dummy input, to prevent flaky typing
        cy.get('div.modal-content').find('input[name="selectorName"]').type('x');

        cy.get('div.modal-content').find('input[name="selectorName"]').clear().type(config[0].selectorName);
        cy.get('div.modal-content').find('select[name="workflows"]').select(config[0].workflows, { force: true });
        cy.get('div.modal-content').find('select[name="projects"]').select(config[0].projects, { force: true });
        cy.get('div.modal-content').find('select[name="referenceGenomes"]')
          .select(config[0].referenceGenomes, { force: true });
        cy.get('div.modal-content').find('select[name="workflowVersions"]').select(config[0].versions, { force: true });
        cy.get('div.modal-content').find('select[name="seqTypes"]').select(config[0].seqType, { force: true });
        cy.get('div.modal-content').find('select[name="libraryPreparationKits"]')
          .select(config[0].libraryPreparationKits, { force: true });
        cy.get('div.modal-content').find('select[name="type"]').select(config[0].type, { force: true });
        cy.get('div.modal-content').find('textarea[name="value"]')
          .type(config[0].value, { parseSpecialCharSequences: false });
        cy.get('div.modal-content').find('#format-button').click();
      });

      cy.get('#save-button').click();

      cy.wait('@createWorkflowConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
    });

    it('should filter the workflow configuration and edit it', () => {
      cy.intercept('/workflowConfig/data*').as('searchWorkflowConfig');
      cy.intercept('/workflowConfig/fragments/*').as('getWorkflowConfigFragments');

      cy.fixture('workflowConfig.json').then((config) => {
        cy.get('div.search-query').find('select[name="projects"]').select(config[1].projects, { force: true });
        cy.get('div.search-query').find('select[name="type"]').select(config[1].types, { force: true });

        cy.get('div.search-query').find('#search-button').click();

        cy.wait('@searchWorkflowConfig').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.data.length).to.eq(3);
        });

        cy.intercept('/workflowConfig/update*').as('updateWorkflowConfig');

        cy.get('table#workflowConfigResult td.dataTables_empty').should('not.exist');
        cy.get('table#workflowConfigResult tbody tr').contains(config[0].selectorName).should('exist').parent()
          .parent()
          .find('#edit-row')
          .click();

        cy.wait('@getWorkflowConfigFragments').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('div.modal-content').should('be.visible');
        cy.get('div.modal-content input[name="selectorName"]').should('have.value', config[0].selectorName);

        // Dummy input, to prevent flaky typing
        cy.get('div.modal-content').find('input[name="selectorName"]').type('x');

        cy.get('div.modal-content input[name="selectorName"]').focus().clear().type(config[2].selectorName);
        cy.get('div.modal-content').find('select[name="workflows"]').select(config[2].workflows, { force: true });
        cy.get('div.modal-content').find('select[name="projects"]').select(config[2].projects, { force: true });
        cy.get('div.modal-content').find('select[name="referenceGenomes"]')
          .select(config[2].referenceGenomes, { force: true });
        cy.get('div.modal-content').find('select[name="workflowVersions"]').select(config[2].versions, { force: true });
        cy.get('div.modal-content').find('select[name="seqTypes"]').select(config[2].seqType, { force: true });
        cy.get('div.modal-content').find('select[name="libraryPreparationKits"]')
          .select(config[2].libraryPreparationKits, { force: true });
        cy.get('div.modal-content').find('select[name="type"]').select(config[2].type, { force: true });
        cy.get('div.modal-content').find('textarea[name="value"]').clear().type(config[2].value);

        cy.get('div.modal-content').find('#save-button').click();

        cy.wait('@updateWorkflowConfig').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
      });
    });

    it('should search for the modified selector and create a new selector based on the modified one', () => {
      // Search for the modified selector
      cy.intercept('/workflowConfig/data*').as('searchWorkflowConfig');
      cy.intercept('/workflowConfig/fragments/*').as('getWorkflowConfigFragments');

      cy.fixture('workflowConfig.json').then((config) => {
        cy.get('div.search-query').find('select[name="projects"]').select(config[2].projects, { force: true });
        cy.get('div.search-query').find('select[name="type"]').select(config[2].type, { force: true });
        cy.get('div.search-query').find('#search-button').click();

        cy.wait('@searchWorkflowConfig').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.data.length).to.eq(1);
        });

        cy.get('table#workflowConfigResult td.dataTables_empty').should('not.exist');

        // create a new selector based on the modified one
        cy.intercept('/workflowConfig/create*').as('createWorkflowConfig');

        cy.get('tr').contains(config[2].selectorName).parent().parent()
          .find('#new-row')
          .click();

        cy.wait('@getWorkflowConfigFragments').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
        cy.get('div.modal-content').should('be.visible');
        cy.get('div.modal-content input[name="selectorName"]').should('have.value', config[2].selectorName);

        // Dummy input, to prevent flaky typing
        cy.get('div.modal-content').find('input[name="selectorName"]').type('x');

        cy.get('div.modal-content input[name="selectorName"]').focus().clear().type(config[3].selectorName);
        cy.get('div.modal-content').find('select[name="projects"]').select(config[3].projects, { force: true });

        cy.get('div.modal-content').find('#save-button').click();

        cy.wait('@createWorkflowConfig').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
      });
    });

    it('should search for the selectors and delete them', () => {
      cy.intercept('/workflowConfig/data*').as('searchWorkflowConfig');
      cy.intercept('/workflowConfig/fragments/*').as('getWorkflowConfigFragments');
      cy.intercept('/workflowConfig/deprecate*').as('deprecateWorkflowConfig');

      cy.fixture('workflowConfig.json').then((config) => {
        cy.get('div.search-query').find('select[name="projects"]').select(config[3].projects, { force: true });
        cy.get('div.search-query').find('select[name="type"]').select(config[2].type, { force: true });

        cy.get('div.search-query').find('#search-button').click();

        cy.wait('@searchWorkflowConfig').then((outerInterception) => {
          expect(outerInterception.response.statusCode).to.eq(200);
          expect(outerInterception.response.body.data.length).to.eq(2);
        });

        cy.get('table#workflowConfigResult td.dataTables_empty').should('not.exist');
        cy.get('table#workflowConfigResult').contains(config[2].selectorName).should('exist');
        cy.get('table#workflowConfigResult').contains(config[3].selectorName).should('exist');

        cy.get('table#workflowConfigResult tr').contains(config[2].selectorName).parent()
          .parent()
          .find('#deprecate-row')
          .click({ force: true });
        cy.wait('@getWorkflowConfigFragments').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          cy.get('div#workflowConfigModal').should('be.visible');
          // Cypress has to wait, until eventListener for closing modal when clicking ok is appended
          cy.wait(20);
          cy.get('div.modal-dialog button#save-button').should('be.visible').click();
        });

        cy.wait('@deprecateWorkflowConfig').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.name).to.eq(config[2].selectorName);
        });

        cy.get('div#workflowConfigModal').should('not.be.visible');
        cy.get('table#workflowConfigResult tr').contains(config[3].selectorName).parent()
          .parent()
          .find('#deprecate-row')
          .click();
        cy.wait('@getWorkflowConfigFragments').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          cy.get('div#workflowConfigModal').should('be.visible');
          // Cypress has to wait, until eventListener for closing modal when clicking ok is appended
          cy.wait(20);
          cy.get('div.modal-dialog button#save-button').should('be.visible').click();
        });
        cy.wait('@deprecateWorkflowConfig').then((interception2) => {
          expect(interception2.response.statusCode).to.eq(200);
          expect(interception2.response.body.name).to.eq(config[3].selectorName);
        });
        cy.get('div#workflowConfigModal').should('not.be.visible');
      });
    });

    it('should show workflowConfigModal on workflowConfig page', () => {
      cy.visit('/workflowConfigViewer/index');
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');
      cy.get('.selector').find('#workflowSelector').select('PanCancer alignment', { force: true });
      cy.wait('@buildConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.get('#relatedSelectors').find('a.above-stretched-link').first().then(($link) => {
        const selectorName = $link.text();
        cy.get('#relatedSelectors').find('a.above-stretched-link').first().should('not.be.empty')
          .click();

        cy.url().should('include', 'selector.id')
          .and('include', '#workflowConfigModal');
        cy.get('div.modal-content').should('be.visible')
          .find('input[name="selectorName"]').should('have.value', selectorName);
      });
    });

    it('should show error toaster if selectorId is wrong', () => {
      cy.visit('/workflowConfigViewer/index');
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');

      cy.get('#otpToastBox').should('not.exist');
      cy.get('.selector').find('#workflowSelector').select('Cell Ranger', { force: true });
      cy.visit('/workflowConfig/index?project=ExampleProject&selector.id=-1#workflowConfigModal');
      cy.get('#otpToastBox').should('exist').and('contain', 'Failed fetching Selector by selectorId: ');
    });
  });
});
