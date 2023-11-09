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

describe('Check workflow config viewer page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/workflowConfigViewer/index');
    });

    it('should load the right configs for selected properties and filter with selected types', () => {
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');

      const workflow = 'PanCancer alignment';

      cy.get('.selector').find('#workflowSelector').select(workflow, { force: true });

      cy.wait('@buildConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#configValue').invoke('val').should('not.be.empty');
        cy.get('div#right-side #relatedSelectors').children().should('have.length', 2);
      });

      cy.get('#libPrepKitSelector').select('Agilent SureSelect V5', { force: true });
      cy.wait('@buildConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#configValue').invoke('val').should('not.be.empty');
        cy.get('div#right-side #relatedSelectors').children().should('have.length', 3);
      });

      cy.get('#relatedSelectorType').select('REVERSE_SEQUENCE', { force: true });
      cy.get('#configValue').invoke('val').should('not.be.empty');
      cy.get('[data-type]:not([data-type="REVERSE_SEQUENCE"])').should('not.be.visible');

      cy.get('#relatedSelectorType + .select2').contains('.select2-selection__choice', 'REVERSE_SEQUENCE')
        .find('.select2-selection__choice__remove').click();
      cy.get('[data-type]:not([data-type="REVERSE_SEQUENCE"])').should('be.visible');

      cy.get('#refGenSelector').select('GRCm38mm10_PhiX', { force: true });
      cy.wait('@buildConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#configValue').invoke('val').should('not.be.empty');
        cy.get('div#right-side #relatedSelectors').children().should('have.length', 4);
      });
    });

    it('should show toaster when no config exists for selector', () => {
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');

      cy.get('#otpToastBox').should('not.exist');
      cy.get('.selector').find('#workflowSelector').select('Bash FastQC', { force: true });
      cy.get('.selector').find('#refGenSelector').select('1KGRef_PhiX', { force: true });
      cy.get('.selector').find('#libPrepKitSelector').select('NEBNext Methyl-seq Kit', { force: true });

      cy.wait('@buildConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#otpToastBox').should('exist').then((toaster) => {
          cy.wrap(toaster).should('contain', 'There are no related selectors to this selection');
        });
        cy.get('#configValue').invoke('val').should('be.empty');
        cy.get('#relatedSelectors').find('div .card').should('have.length', 0);
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/workflowConfigViewer/index');
      cy.checkAccessDenied('/workflowConfigViewer/build');
    });
  });
});
