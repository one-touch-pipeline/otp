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

describe('Check workflow config viewer page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/workflowConfigViewer/index');
    });

    it('should load config and related selectors when config exists', () => {
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');

      cy.get('.selector').find('#workflowSelector').select('Cell Ranger', { force: true });

      cy.wait('@buildConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#configValue').invoke('val').should('not.be.empty');
        cy.get('#relatedSelectors').find('div .card').should('have.length', 10).then(
          (relatedSelectors) => {
            cy.wrap(relatedSelectors['0']).find('pre').should('not.be.visible');
            cy.wrap(relatedSelectors['0']).find('.code > a').click({ force: true });
            cy.wrap(relatedSelectors['0']).find('pre').should('be.visible');
            cy.wrap(relatedSelectors['0']).find('pre').should('not.be.empty');
          }
        );
        cy.get('#relatedSelectorType').select('BED_FILE', { force: true });
        cy.get('#relatedSelectors').find('div .card').filter(':visible').should('have.length', 4);
        cy.get('#relatedSelectorType').invoke('val', ''); // to override selected elements
        cy.get('#relatedSelectorType').select(['BED_FILE', 'REVERSE_SEQUENCE'], { force: true });
        cy.get('#relatedSelectors').find('div .card').filter(':visible').should('have.length', 7);
      });
    });

    it('should show toaster when no config exists for selector', () => {
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');

      cy.get('#otpToastBox').should('not.exist');
      cy.get('.selector').find('#projectSelector').select('Example project 1', { force: true });

      cy.wait('@buildConfig').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#otpToastBox').should('exist').then((toaster) => {
          cy.wrap(toaster).should('contain', 'There are no related selectors to this selection');
        });
        cy.get('#configValue').invoke('val').should('be.empty');
        cy.get('#relatedSelectors').find('div .card').should('have.length', 0);
      });
    });
    it('should show workflowConfigModal on workflowConfig page', () => {
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');

      cy.get('.selector').find('#workflowSelector').select('Cell Ranger', { force: true });

      cy.get('#relatedSelectors').find('a.above-stretched-link').first().then(($link) => {
        const selectorName = $link.text();
        cy.get('#relatedSelectors').find('a.above-stretched-link').first().click()
          .should('not.be.empty');
        cy.url().should('include', 'selector.id')
          .should('include', '#workflowConfigModal');
        cy.get('div.modal-content').should('be.visible');
        cy.get('div.modal-content').find('input[name="selectorName"]').should('have.value', selectorName);
      });
    });
    it('should show toaster if selectorId does not match', () => {
      cy.intercept('/workflowConfigViewer/build*').as('buildConfig');

      cy.get('#otpToastBox').should('not.exist');
      cy.get('.selector').find('#workflowSelector').select('Cell Ranger', { force: true });
      cy.visit('http://localhost:8080/workflowConfig/index?project=ExampleProject&selector.id=-1#workflowConfigModal');
      cy.get('#otpToastBox').should('exist').then((toaster) => {
        cy.wrap(toaster).should('contain', 'Failed fetching Selector by selectorId: ');
      });
    });
  });
});
