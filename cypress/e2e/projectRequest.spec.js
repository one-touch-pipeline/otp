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

describe('Check projectRequest page', () => {
  'use strict';

  context('when user is a default user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it.skip('should add project request as draft', () => {
      cy.intercept('/projectRequest/index*').as('saveProjectRequest');
      cy.visit('/projectRequest/index');

      cy.fixture('projectRequest.json').then((request) => {
        cy.get('select#projectType').select(request[0].projectType, { force: true });
        cy.get('input#name').type(request[0].projectName);
        cy.get('textarea#description').type(request[0].description);
        cy.get('input#approxNoOfSamples').type(request[0].approximateNumberOfSamples);
        cy.get('select#keywords').parent().find('input').type(request[0].keywords, { force: true });
        cy.get('select#storagePeriod').select(request[0].storagePeriod, { force: true });
        cy.get('select#speciesWithStrainList').select(request[0].speciesWithStrainList, { force: true });
        cy.get('select#seqTypesList').select(0, { force: true });

        const operatorUsername = Cypress.env('operator_username');
        cy.get('input.username-input').first().type(operatorUsername);
        cy.get('select.project-role-select').first().select('BIOINFORMATICIAN', { force: true });

        const username = Cypress.env('user_username');
        cy.get('select.pi-user-select').eq(1).select(username);
        cy.get('select.project-role-select').eq(1).select('PI', { force: true });

        cy.get('input[name=_action_saveIndex]').click();

        cy.wait('@saveProjectRequest').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        });
      });
    });

    it.skip('should find, edit and save unresolved project request', () => {
      cy.intercept('/projectRequest/unresolved*').as('routeToUnresolved');
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/index');

      // navigate to the unresolved tab
      cy.get('.nav-link').contains('Unresolved').click();
      cy.wait('@routeToUnresolved').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);

        // select the project request in the table
        cy.get('table tbody tr').first().find('td').first()
          .find('a')
          .click();

        // edit a field of the project request
        cy.get('input[value=Edit]').click();
        cy.wait('@projectRequestIndex').then((intcpt) => {
          expect(intcpt.response.statusCode).to.eq(200);
          cy.location('pathname').should('eq', '/projectRequest/index');
          cy.get('textarea#requesterComment').type('hello world');
          cy.get('input#saveIndex-request-btn').click();
          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        });
      });
    });

    it.skip('should submit an unresolved project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      // edit a field of the project request
      cy.get('input#submitView-request-btn').click();
      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('eq', '/projectRequest/unresolved');
        cy.get('table tbody tr').first().find('td').should('contain', 'Check by Data Manager needed');
      });
    });
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it.skip('should pass on the project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.fixture('projectRequest.json').then((request) => {
        cy.get('table tbody tr').first().find('td').first()
          .should('contain', request[0].projectName).click();
      });

      cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      cy.get('input#passOn-request-btn').click();

      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      });
    });
  });

  context('when user is a PI', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it.skip('should edit and save the project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      cy.get('input#edit-request-btn').click();
      cy.location('pathname').should('match', /^\/projectRequest\/index/);

      cy.fixture('projectRequest.json').then((request) => {
        cy.get('select#speciesWithStrainList').parent().find('input')
          .type(request[1].speciesWithStrainList, { force: true });
      });

      cy.get('input#saveIndex-request-btn').click();
    });

    it.skip('should delete a project request being edited', () => {
      cy.intercept('/projectRequest/delete*').as('deleteProjectRequest');
      cy.visit('/projectRequest/unresolved');
      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      cy.get('input#edit-request-btn').click();

      cy.visit('/projectRequest/unresolved');
      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      cy.get('#delete-request-btn').click();
      cy.get('#confirmModal').should('be.visible').click();

      cy.wait('@deleteProjectRequest').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.location('pathname').should('eq', '/projectRequest/unresolved');
        cy.fixture('projectRequest.json').then((request) => {
          cy.get('body').should('not.contain', request[0].projectName);
        });
      });
    });
  });

  context('when user is a default user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it.skip('should submit a project request', () => {
      cy.intercept('/projectRequest/index*').as('saveProjectRequest');
      cy.visit('/projectRequest/index');

      cy.fixture('projectRequest.json').then((requests) => {
        const request = requests[2];
        cy.get('select#projectType').select(request.projectType, { force: true });
        cy.get('input#name').type(request.projectName);
        cy.get('textarea#description').type(request.description);
        cy.get('input#approxNoOfSamples').type(request.approximateNumberOfSamples);
        cy.get('select#keywords').parent().find('input').type(request.keywords, { force: true });
        cy.get('select#storagePeriod').select(request.storagePeriod, { force: true });
        cy.get('select#speciesWithStrainList').select(request.speciesWithStrainList, { force: true });
        cy.get('select#seqTypesList').select(0, { force: true });

        const username = Cypress.env('user_username');
        cy.get('input.username-input').eq(0).type(username);
        cy.get('select.project-role-select').eq(0).select('PI', { force: true });

        cy.get('input#submitIndex-request-btn').click();

        cy.wait('@saveProjectRequest').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          cy.location('pathname').should('match', /^\/projectRequest\/unresolved/);
        });
      });
    });
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it.skip('should pass on the project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.fixture('projectRequest.json').then((request) => {
        cy.get('table tbody tr').first().find('td').first()
          .should('contain', request[0].projectName)
          .click();
      });

      cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      cy.get('input#passOn-request-btn').click();

      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      });
    });
  });

  context('when user is a PI', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it.skip('should approve the project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      cy.get('input#confirmConsent').check();
      cy.get('input#confirmRecordOfProcessingActivities').check();
      cy.get('input[name=_action_approve]').click();

      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        cy.get('h3').contains('Approved').should('exist');
      });
    });
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it.skip('should delete the project request', () => {
      cy.intercept('/projectRequest/delete*').as('deleteProjectRequest');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      cy.get('#delete-request-btn').click();
      cy.get('#confirmModal').should('be.visible').click();

      cy.wait('@deleteProjectRequest').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.location('pathname').should('eq', '/projectRequest/unresolved');
        cy.fixture('projectRequest.json').then((request) => {
          cy.get('body').should('not.contain', request[0].projectName);
        });
      });
    });
  });
});
