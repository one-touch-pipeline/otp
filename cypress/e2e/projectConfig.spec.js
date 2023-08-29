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

describe('Check projectConfig page', () => {
  'use strict';

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not see the edit button on the index page', () => {
      cy.visit('/projectConfig/index');
      cy.get('button.edit').should('not.exist');
    });
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.intercept('/projectConfig/updateProjectField*').as('updateProjectField');
      cy.visit('/projectConfig/index');
    });

    /**
     * Should
     *  - change the unix group back to developer
     */
    after(() => {
      cy.intercept('/projectConfig/updateUnixGroup*').as('updateUnixGroup');

      const cellKey = 'Unix Group';
      const unixGroup = 'developer';

      cy.get('td').contains(cellKey).siblings().last()
        .find('#button-edit-unixGroup')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('#unixGroupInput')
        .clear()
        .type(unixGroup);
      cy.get('td').contains(cellKey).siblings().last()
        .find('#button-save-unixGroup')
        .click();

      cy.wait('@updateUnixGroup').its('response.statusCode').should('eq', 200);
    });

    it('should add a comment', () => {
      cy.intercept('/projectConfig/saveProjectComment*').as('saveProjectComment');

      cy.get('#comment-content').clear().type(`Hello World, this is a test comment: ${Cypress._.random(0, 1e6)}`);
      cy.get('#comment-box-container #button-save').click();

      cy.wait('@saveProjectComment').its('response.statusCode').should('eq', 200);
    });

    it('should update the project type', () => {
      const cellKey = 'Project Type';

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectField').its('response.statusCode').should('eq', 200);
    });

    it('should update the analysis directory', () => {
      const cellKey = 'Analysis Directory';
      const analysisDir = `/tmp/dir/${Cypress._.random(0, 1e6)}`;

      cy.intercept('/projectConfig/updateAnalysisDir*').as('updateAnalysisDir');

      cy.get('td').contains(cellKey).siblings().last()
        .find('#button-edit-analysisDir')
        .click();

      cy.get('td').contains(cellKey).siblings().last()
        .find('#analysisDirInput')
        .clear()
        .type(analysisDir);

      cy.get('td').contains(cellKey).siblings().last()
        .find('#button-save-analysisDir')
        .click();

      cy.wait('@updateAnalysisDir').its('response.statusCode').should('eq', 200);
    });

    it('should update the unix group', () => {
      const cellKey = 'Unix Group';
      const unixGroup = 'wheel';

      cy.intercept('/projectConfig/updateUnixGroup*').as('updateUnixGroup');

      cy.get('td').contains(cellKey).siblings().last()
        .find('#button-edit-unixGroup')
        .click();

      cy.get('td').contains(cellKey).siblings().last()
        .find('#unixGroupInput')
        .clear()
        .type(unixGroup);

      cy.get('td').contains(cellKey).siblings().last()
        .find('#button-save-unixGroup')
        .click();

      cy.wait('@updateUnixGroup').its('response.statusCode').should('eq', 200);
    });

    it('should update the description', () => {
      const cellKey = 'Description';

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.js-edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('textarea')
        .type(`Random sample text: ${Cypress._.random(0, 1e6)}`);
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectField').its('response.statusCode').should('eq', 200);
    });

    it('should update the related projects', () => {
      const cellKey = 'Related Projects';

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('input.edit-switch-input')
        .type(`Random sample text: ${Cypress._.random(0, 1e6)}`);
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectField').its('response.statusCode').should('eq', 200);
    });

    it('should update the species with strain', () => {
      const cellKey = 'Species [with Strain]';

      cy.intercept('/projectConfig/updateSpeciesWithStrains*').as('updateSpeciesWithStrains');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(0, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateSpeciesWithStrains').its('response.statusCode').should('eq', 200);
    });

    it('should update the storage until date', () => {
      const cellKey = 'Storage Until';

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('input[type=date]')
        .type('2022-01-01');
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectField').its('response.statusCode').should('eq', 200);
    });

    it('should update the name in metadata files', () => {
      const cellKey = 'Name in Metadata Files';

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('input[type=text]')
        .type(`Random sample text: ${Cypress._.random(0, 1e6)}`);
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectField').its('response.statusCode').should('eq', 200);
    });

    it('should update the processing priority', () => {
      const cellKey = 'Processing Priority';

      cy.intercept('/projectConfig/updateProcessingPriority*').as('updateProcessingPriority');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProcessingPriority').its('response.statusCode').should('eq', 200);
    });

    it('should update the group', () => {
      const cellKey = /^Group$/;

      cy.intercept('/projectConfig/updateProjectGroup*').as('updateProjectGroup');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(0, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectGroup').its('response.statusCode').should('eq', 200);
    });

    it('should update the sample parser', () => {
      const cellKey = 'Sample Parser';

      cy.intercept('/projectConfig/updateSampleIdentifierParserBeanName*').as('updateSampleIdentifierParserBeanName');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateSampleIdentifierParserBeanName').its('response.statusCode').should('eq', 200);
    });

    it('should update the finger printing flag', () => {
      const cellKey = 'Finger Printing';

      cy.intercept('/projectConfig/updateFingerPrinting*').as('updateFingerPrinting');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateFingerPrinting').its('response.statusCode').should('eq', 200);
    });

    it('should update the processing notification flag', () => {
      const cellKey = 'Send Processing Notifications';

      cy.intercept('/projectConfig/updateProcessingNotification*').as('updateProcessingNotification');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProcessingNotification').its('response.statusCode').should('eq', 200);
    });

    it('should update the qc traffic light notification flag', () => {
      const cellKey = 'Send QC status warning notifications';

      cy.intercept('/projectConfig/updateQcTrafficLightNotification*').as('updateQcTrafficLightNotification');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateQcTrafficLightNotification').its('response.statusCode').should('eq', 200);
    });

    it('should update the public flag', () => {
      const cellKey = 'Public';

      cy.intercept('/projectConfig/updatePubliclyAvailable*').as('updatePubliclyAvailable');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updatePubliclyAvailable').its('response.statusCode').should('eq', 200);
    });

    it('should update the closed flag', () => {
      const cellKey = 'Closed';

      cy.intercept('/projectConfig/updateClosed*').as('updateClosed');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(1, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateClosed').its('response.statusCode').should('eq', 200);
    });

    it('should update the internal notes', () => {
      const cellKey = 'Internal Notes';

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.js-edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('textarea')
        .type(`Random sample text: ${Cypress._.random(0, 1e6)}`);
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectField').its('response.statusCode').should('eq', 200);
    });

    it('should update the tumor entity', () => {
      const cellKey = 'Tumor Entity';

      cy.intercept('/projectConfig/updateTumorEntity*').as('updateTumorEntity');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(0, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateTumorEntity').its('response.statusCode').should('eq', 200);
    });

    it('should update the end date', () => {
      const cellKey = 'End Date';

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('input[type=date]')
        .type('2022-01-01');
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateProjectField').its('response.statusCode').should('eq', 200);
    });

    it('should update the project request available flag', () => {
      const cellKey = 'Project Request Available';

      cy.intercept('/projectConfig/updateRequestAvailable*').as('updateRequestAvailable');

      cy.get('td').contains(cellKey).siblings().last()
        .find('button.edit')
        .click();
      cy.get('td').contains(cellKey).siblings().last()
        .find('select')
        .select(0, { force: true });
      cy.get('td').contains(cellKey).siblings().last()
        .find('button.save')
        .click();

      cy.wait('@updateRequestAvailable').its('response.statusCode').should('eq', 200);
    });
  });
});
