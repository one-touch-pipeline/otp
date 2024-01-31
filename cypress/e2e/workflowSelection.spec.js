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

describe('Check workflow selection page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should be able to change merging criteria', () => {
      cy.visit('/workflowSelection/index');

      cy.get('table#mergingCriteria .edit-switch').should('exist');
      cy.get('table#mergingCriteria .edit-switch').first().find('button.edit').click();
      cy.get('table#mergingCriteria .edit-switch').first().find('select').select('No', { force: true });
      cy.get('table#mergingCriteria .edit-switch').first().find('button.save').click();
      cy.get('table#mergingCriteria .edit-switch').first().find('.edit-switch-label').should('contain.text', 'No');
    });

    it('should create alignment configuration and delete it', () => {
      cy.visit('/workflowSelection/index');

      cy.intercept('/workflowSelection/possibleAlignmentOptions*').as('possibleOptions');
      cy.intercept('/workflowSelection/saveAlignmentConfiguration*').as('createConfig');

      cy.fixture('workflowSelection.json').then((config) => {
        cy.get('#alignment-workflow-select').select(config.workflow, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#alignment-seq-type-select').select(config.seqType, { force: true });
        cy.wait('@possibleOptions');
        cy.log(config.version);
        cy.get('#alignment-version-select').select(config.version.id, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#alignment-ref-genome-select').select(config.refGenome, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#alignment-species-select').select(config.species, { force: true });
        cy.wait('@possibleOptions');

        cy.get('button.add-alignment-config-btn').click();
        cy.wait('@createConfig').then((interception) => {
          expect(interception.response.statusCode).equal(200);
          const wvSelectorId = interception.response.body.workflowVersionSelector.id;
          const rgSelectorId = interception.response.body.refGenSelectorId;
          const btnSelector = `button[data-version-selector=${wvSelectorId}][data-ref-genome-selector=${rgSelectorId}]`;
          cy.get(`table#alignment ${btnSelector}`).as('removeBtn');
          cy.get('@removeBtn').parent().parent().find('td')
            .as('createdCells');

          cy.get('@createdCells').contains(config.workflow);
          cy.get('@createdCells').contains(config.seqType);
          cy.get('@createdCells').contains(config.version.name);
          cy.get('@createdCells').contains(config.refGenome);
          cy.get('@createdCells').contains(config.species[0]);
          cy.get('@createdCells').contains(config.species[1]);
        });
      });

      // Should delete the created row again
      cy.intercept('/workflowSelection/deleteAlignmentConfiguration*').as('deleteConfig');

      cy.get('@removeBtn').click();
      cy.wait('@deleteConfig').then((interception) => {
        expect(interception.response.statusCode).equal(200);

        cy.get('@removeBtn').should('not.exist');
      });
    });

    it('should throw error, when species doesn\'t match reference genome', () => {
      cy.visit('/workflowSelection/index');

      cy.intercept('/workflowSelection/saveAlignmentConfiguration*').as('createConfig');

      cy.fixture('workflowSelection.json').then((config) => {
        cy.get('#alignment-workflow-select').select(config.workflow, { force: true });
        cy.get('#alignment-seq-type-select').select(config.seqType, { force: true });
        cy.get('#alignment-version-select').select(config.version.id, { force: true });
        cy.get('#alignment-ref-genome-select').select(config.refGenome, { force: true });
        cy.get('#alignment-species-select').select(config.species[0], { force: true });

        cy.get('button.add-alignment-config-btn').click();
        cy.wait('@createConfig').then((interception) => {
          expect(interception.response.statusCode).equal(400);
        });
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should be able to see workflow versions and merging criteria, but no buttons to change them', () => {
      cy.visit('/workflowSelection/index');

      cy.get('table#alignment .edit-switch').should('not.exist');
      cy.get('table#alignment tr').should('have.length.greaterThan', 3);

      cy.get('table#mergingCriteria .edit-switch').should('not.exist');
      cy.get('table#mergingCriteria tr').should('have.length.greaterThan', 3);

      cy.get('#alignment-workflow-select').should('not.exist');
      cy.get('#alignment-seq-type-select').should('not.exist');
      cy.get('#alignment-version-select').should('not.exist');
      cy.get('#alignment-ref-genome-select').should('not.exist');
      cy.get('#alignment-species-select').should('not.exist');
      cy.get('#remove-alignment-config-btn').should('not.exist');
    });
  });
});
