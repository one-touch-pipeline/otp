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

describe('Check projectRequest page', () => {
  'use strict';

  context('when user is a default user', () => {
    before(() => {
      cy.logout();
    });

    beforeEach(() => {
      cy.loginAsUser();
      cy.visit('/projectRequest/index');
    });

    it('should add project request as draft', () => {
      cy.intercept('/projectRequest/index*').as('saveProjectRequest');

      cy.fixture('projectRequest.json').then((request) => {
        cy.get('select#projectType').select(request.projectType, { force: true });
        cy.get('input#name').type(request.projectName);
        cy.get('textarea#description').type(request.description);
        cy.get('input#approxNoOfSamples').type(request.approximateNumberOfSamples);
        cy.get('select#keywords').select(0, { force: true });
        cy.get('select#storagePeriod').select(request.storagePeriod, { force: true });
        cy.get('select#speciesWithStrainList').select(request.speciesWithStrainList, { force: true });
        cy.get('select#seqTypesList').select(0, { force: true });
        cy.get('input.username-input').first().type('otp');
        cy.get('select.project-role-select').first().select('PI', { force: true });
        cy.get('input[name=_action_saveIndex]').click();

        cy.wait('@saveProjectRequest').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        });
      });
    });

    it('should find, edit and save unresolved project request', () => {
      cy.intercept('/projectRequest/unresolved*').as('routeToUnresolved');
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');

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
          expect(intcpt.response.statusCode).to.eq(302);
          cy.location('pathname').should('eq', '/projectRequest/index');
          cy.get('textarea#requesterComment').type('hello world');
          cy.get('input[name=_action_saveIndex]').click();
          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        });
      });
    });

    it('should submit an unresolved project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      // edit a field of the project request
      cy.get('input[value=Submit]').click();
      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('eq', '/projectRequest/unresolved');
        cy.get('table tbody tr').first().find('td').should('contain', 'Check by Data Manager needed');
      });
    });
  });

  context('when user is a PI', () => {
    before(() => {
      cy.logout();
    });

    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should find the project request', () => {
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.fixture('projectRequest.json').then((request) => {
        cy.get('table tbody tr').first().find('td').first()
          .should('contain', request.projectName);
      });
    });

    it('should pass on the project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      cy.get('input[name=_action_passOn]').click();

      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        cy.get('input[name=_action_approve]').should('exist');
      });
    });

    it('should approve the project request', () => {
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

    it('should delete the project request', () => {
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');

      // select the project request in the table
      cy.get('table tbody tr').first().find('td').first()
        .find('a')
        .click();

      cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      cy.get('input[name=_action_delete]').click();

      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('eq', '/projectRequest/unresolved');
        cy.fixture('projectRequest.json').then((request) => {
          cy.get('body').should('not.contain', request.projectName);
        });
      });
    });
  });
});
