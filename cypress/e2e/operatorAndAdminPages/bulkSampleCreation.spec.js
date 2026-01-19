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

describe('Check bulk sample creation page', () => {
  'use strict';
  const fixturesFolder = Cypress.config('fixturesFolder');

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/bulkSampleCreation/index');
    });


    it('should visit the index page', () => {
      cy.get('#select2-project-container').should('have.text', 'ExampleProject');
      cy.get('tr:first-child').should('contain.text', 'ExampleProject');
      cy.get('#sampleText').should('have.value', 'PROJECT\tPID\tSAMPLE_TYPE\tSAMPLE_IDENTIFIER');

      cy.get('#project.form-select').select('Example project 1', { force: true });
      cy.url().should('include', 'Example+project+1');
      cy.get('tr:first-child').should('contain.text', 'Example project 1');
    });

    it('should have the default delimiter set to tab', () => {
      cy.get('select#delimiter').find('option:selected').should('have.text', 'tab');
    });

    it('should be able to upload samples for bulk creation', () => {
      cy.intercept('/bulkSampleCreation/upload*').as('bulkSampleUpload');
      cy.intercept('/bulkSampleCreation/submit*').as('bulkSampleCreation');

      cy.get('.form-control[name=content]').selectFile(`${fixturesFolder}/bulkSampleCreation.txt`);
      cy.get('.btn').contains('Upload').click();

      cy.wait('@bulkSampleUpload').its('response.statusCode').should('eq', 302);
      cy.fixture('bulkSampleCreation.txt').then((exampleSamples) => {
        cy.get('#sampleText').should('have.value', exampleSamples);
      });

      cy.get('select#delimiter').select(',', { force: true });
      cy.get('.btn').contains('Submit').click();

      cy.wait('@bulkSampleCreation').its('response.statusCode').should('eq', 302);
      cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain.text', 'Creation succeeded');
    });


    it('should be able to use different delimiters', () => {
      checkDifferentDelimiters(';', ';');
      checkDifferentDelimiters('tab', '\t');
      checkDifferentDelimiters('space', ' ');
    });


    it('cannot create samples without delimiter or with unknown project', () => {
      cy.get('#sampleText').clear().type('PROJECT,PID,SAMPLE_TYPE,SAMPLE_IDENTIFIER\nUnknownProject,pid1,tumor1,identifier1');

      cy.get('.btn').contains('Submit').click();

      // Fixing the delimiter and trying again
      cy.get('select#delimiter').select(',', { force: true });
      cy.get('.btn').contains('Submit').click();

      cy.get('#otpToastBox .otpErrorToast').should('exist')
        .and('contain.text', 'Creation failed')
        .and('contain.text', 'Could not find Project');
    });


    it('can create samples without project but needs to create missing sample types', () => {
      cy.intercept('/bulkSampleCreation/submit*').as('bulkSampleCreation');

      cy.get('#sampleText').clear().type('PID,SAMPLE_TYPE,SAMPLE_IDENTIFIER\npid1,tumor1,identifier1');
      cy.get('select#delimiter').select(',', { force: true });
      cy.get('.btn').contains('Submit').click();

      cy.wait('@bulkSampleCreation').its('response.statusCode').should('eq', 302);
      cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain.text', 'Creation succeeded');
    });


    it('cannot create samples with the same sample name in different projects', () => {
      cy.get('#sampleText').clear()
        .type('PROJECT,PID,SAMPLE_TYPE,SAMPLE_IDENTIFIER\n"Project E2E",TP01-00001,CELL01,TP01-00001\n,TP01-00001,CELL01,TP01-00001');
      cy.get('select#delimiter').select(',', { force: true });
      cy.get('.btn').contains('Submit').click();

      cy.get('#otpToastBox .otpErrorToast').should('exist')
        .and('contain.text', 'Creation failed')
        .and('contain.text', 'The sample name already exists, but is connected to project \'Project E2E\' and not \'ExampleProject\'');
    });
  });



  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/bulkSampleCreation/index');
    });
  });
});


function checkDifferentDelimiters(delimiter, delimiterValue) {
  cy.intercept('/bulkSampleCreation/submit*').as('bulkSampleCreation');

  cy.fixture('bulkSampleCreation.txt').then((exampleSamples) => {
    cy.get('#sampleText').clear().type(exampleSamples.replaceAll(',', delimiterValue));
  });
  cy.get('select#delimiter').select(delimiter, { force: true });
  cy.get('.btn').contains('Submit').click();

  cy.wait('@bulkSampleCreation').its('response.statusCode').should('eq', 302);
  cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain.text', 'Creation succeeded');
}
