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
      cy.loginAs('operator');
    });

    it('should be able to change merging criteria', () => {
      cy.visit('/workflowSelection/index');

      cy.get('h2#headingCriteria').click();
      cy.get('table#mergingCriteriaTable .edit-switch').should('exist');
      cy.get('table#mergingCriteriaTable .edit-switch').first().find('button.edit').click();
      cy.get('table#mergingCriteriaTable .edit-switch').first().find('select').select('No', { force: true });
      cy.get('table#mergingCriteriaTable .edit-switch').first().find('button.save').click();
      cy.get('table#mergingCriteriaTable .edit-switch').first().find('.edit-switch-label').should('contain.text', 'No');
    });

    it('should create alignment configuration and delete it', () => {
      cy.visit('/workflowSelection/index');

      cy.intercept('/workflowSelection/possibleAlignmentOptions*').as('possibleOptions');
      cy.intercept('/workflowSelection/saveAlignmentConfiguration*').as('createConfig');

      cy.get('h2#headingAlignment').click();

      cy.fixture('workflowSelection.json').then((config) => {
        cy.get('#alignment-workflow-select').select(config.alignment.workflow, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#alignment-seq-type-select').select(config.alignment.seqType, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#alignment-version-select').select(config.alignment.version.id, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#alignment-ref-genome-select').select(config.alignment.refGenome, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#alignment-species-select').select(config.alignment.species, { force: true });
        cy.wait('@possibleOptions');

        cy.get('button#add-alignment-config-btn').click();
        cy.wait('@createConfig').then((interception) => {
          expect(interception.response.statusCode).equal(200);
          const wvSelectorId = interception.response.body.workflowVersionSelector.id;
          const rgSelectorId = interception.response.body.refGenSelectorId;
          const btnSelector = `button[data-version-selector=${wvSelectorId}][data-ref-genome-selector=${rgSelectorId}]`;
          cy.get(`table#alignmentTable ${btnSelector}`).as('removeBtn');
          cy.get('@removeBtn').parent().parent().find('td')
            .as('createdCells');

          cy.get('@createdCells').contains(config.alignment.workflow);
          cy.get('@createdCells').contains(config.alignment.seqType);
          cy.get('@createdCells').contains(config.alignment.version.name);
          cy.get('@createdCells').contains(config.alignment.refGenome);
          cy.get('@createdCells').contains(config.alignment.species[0]);
          cy.get('@createdCells').contains(config.alignment.species[1]);
        });
      });

      // Should delete the created row again
      cy.intercept('/workflowSelection/deleteConfiguration*').as('deleteConfig');

      cy.get('@removeBtn').click();
      cy.wait('@deleteConfig').then((interception) => {
        expect(interception.response.statusCode).equal(200);

        cy.get('@removeBtn').should('not.exist');
      });
    });

    it('should create two analysis configurations for same project and seq type and delete it', () => {
      cy.visit('/workflowSelection/index');

      cy.intercept('/workflowSelection/possibleAnalysisOptions*').as('possibleOptions');
      cy.intercept('/workflowSelection/saveAnalysisConfiguration*').as('createConfig');

      cy.get('h2#headingAnalysis').click();

      cy.fixture('workflowSelection.json').then((config) => {
        cy.get('#analysis-workflow-select').select(config.analysis[0].workflow, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#analysis-seq-type-select').select(config.analysis[0].seqType, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#analysis-version-select').select(config.analysis[0].version.id, { force: true });
        cy.wait('@possibleOptions');

        cy.get('button#add-analysis-config-btn').click();
        cy.wait('@createConfig').then((interception) => {
          expect(interception.response.statusCode).equal(200);
          const wvSelectorId1 = interception.response.body.workflowVersionSelector.id;
          const btnSelector1 = `button[data-version-selector=${wvSelectorId1}]`;
          cy.get(`table#analysisTable ${btnSelector1}`).as('removeBtn1');
          cy.get('@removeBtn1').parent().parent().find('td')
            .as('createdCells1');
        });

        // Create a second workflowVersionSelector with same project and seqType
        cy.get('#analysis-workflow-select').select(config.analysis[1].workflow, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#analysis-seq-type-select').select(config.analysis[1].seqType, { force: true });
        cy.wait('@possibleOptions');
        cy.get('#analysis-version-select').select(config.analysis[1].version.id, { force: true });
        cy.wait('@possibleOptions');

        cy.get('button#add-analysis-config-btn').click();
        cy.wait('@createConfig').then((interception2) => {
          expect(interception2.response.statusCode).equal(200);
          const wvSelectorId2 = interception2.response.body.workflowVersionSelector.id;
          const btnSelector2 = `button[data-version-selector=${wvSelectorId2}]`;
          cy.get(`table#analysisTable ${btnSelector2}`).as('removeBtn2');
          cy.get('@removeBtn2').parent().parent().find('td')
            .as('createdCells2');
        });

        cy.get('@createdCells1').contains(config.analysis[0].workflow);
        cy.get('@createdCells1').contains(config.analysis[0].seqType);
        cy.get('@createdCells1').contains(config.analysis[0].version.name);

        cy.get('@createdCells2').contains(config.analysis[1].workflow);
        cy.get('@createdCells2').contains(config.analysis[1].seqType);
        cy.get('@createdCells2').contains(config.analysis[1].version.name);
      });


      // Should delete the created rows again
      cy.intercept('/workflowSelection/deleteConfiguration*').as('deleteConfig');

      cy.get('@removeBtn1').click();
      cy.wait('@deleteConfig').then((interception) => {
        expect(interception.response.statusCode).equal(200);

        cy.get('@removeBtn1').should('not.exist');
      });

      cy.get('@removeBtn2').click();
      cy.wait('@deleteConfig').then((interception) => {
        expect(interception.response.statusCode).equal(200);

        cy.get('@removeBtn2').should('not.exist');
      });
    });

    it('should throw error while saving alignment config, when species doesnt match reference genome', () => {
      cy.visit('/workflowSelection/index');
      cy.intercept('/workflowSelection/saveAlignmentConfiguration*').as('createConfig');

      cy.get('#headingAlignment').click();

      cy.fixture('workflowSelection.json').then((config) => {
        cy.get('#alignment-workflow-select').select(config.alignment.workflow, { force: true });
        cy.get('#alignment-seq-type-select').select(config.alignment.seqType, { force: true });
        cy.get('#alignment-version-select').select(config.alignment.version.id, { force: true });
        cy.get('#alignment-ref-genome-select').select(config.alignment.refGenome, { force: true });
        cy.get('#alignment-species-select').select(config.alignment.species[0], { force: true });

        cy.get('button#add-alignment-config-btn').click();
        cy.wait('@createConfig').then((interception) => {
          expect(interception.response.statusCode).equal(400);
        });
      });
    });
  });

  context('when user is normal user with project access', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should be able to see workflow versions and merging criteria, but no buttons to change them', () => {
      cy.visit('/workflowSelection/index');

      cy.get('table#alignmentTable button').should('not.exist');
      cy.get('table#alignmentTable tr').should('have.length.greaterThan', 3);

      cy.get('table#analysisTable button').should('not.exist');
      cy.get('table#analysisTable tr').should('have.length.greaterThan', 1);

      cy.get('table#mergingCriteriaTable .edit-switch').should('not.exist');
      cy.get('table#mergingCriteriaTable button').should('not.exist');
      cy.get('table#mergingCriteriaTable tr').should('have.length.greaterThan', 3);

      cy.get('#alignment-workflow-select').should('not.exist');
      cy.get('#alignment-seq-type-select').should('not.exist');
      cy.get('#alignment-version-select').should('not.exist');
      cy.get('#alignment-ref-genome-select').should('not.exist');
      cy.get('#alignment-species-select').should('not.exist');
      cy.get('#analysis-workflow-select').should('not.exist');
      cy.get('#analysis-seq-type-select').should('not.exist');
      cy.get('#analysis-version-select').should('not.exist');
      cy.get('#analysis-ref-genome-select').should('not.exist');
      cy.get('#analysis-species-select').should('not.exist');
      cy.get('.remove-config-btn').should('not.exist');
      cy.get('#add-alignment-config-btn').should('not.exist');
      cy.get('#add-analysis-config-btn').should('not.exist');
    });
  });
});
