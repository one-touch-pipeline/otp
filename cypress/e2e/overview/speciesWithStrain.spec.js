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

describe('test for species with strain page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.fixture('speciesWithStrain').as('fixture');
      cy.visit('/speciesWithStrain/index');
      cy.intercept('/speciesWithStrain/index*').as('reload');
    });

    it('should create a new strain', () => {
      cy.intercept('/speciesWithStrain/createStrain*').as('createStrain');
      cy.fixture('speciesWithStrain').then((fixture) => {
        cy.get('input#strain-input').clear();
        cy.get('input#strain-input').type(fixture.strainName);
        cy.get('input#create-strain').click();

        cy.wait('@createStrain').its('response.statusCode').should('eq', 302);
        cy.wait('@reload').its('response.statusCode').should('eq', 200);

        cy.get('#strains-list .list-group-item').contains(fixture.strainName);
        checkForSuccessToast();
      });
    });

    it('should create a new species', () => {
      cy.intercept('/speciesWithStrain/createSpecies*').as('createSpecies');
      cy.fixture('speciesWithStrain').then((fixture) => {
        cy.get('#common-name-input').clear();
        cy.get('#common-name-input').type(fixture.commonName);
        cy.get('#scientific-name-input').clear();
        cy.get('#scientific-name-input').type(fixture.scientificName);

        cy.get('#create-scientific-name').click();
        cy.wait('@createSpecies').its('response.statusCode').should('eq', 302);
        cy.wait('@reload').its('response.statusCode').should('eq', 200);

        cy.get('#species-list .list-group-item').contains(fixture.commonName).parent()
          .parent()
          .contains(fixture.scientificName);
        checkForSuccessToast();
      });
    });

    it('should create a new species with strain', () => {
      cy.intercept('/speciesWithStrain/createSpeciesWithStrain*').as('createSpeciesWithStrain');
      cy.fixture('speciesWithStrain').then((fixture) => {
        cy.get('select#species-select').select(fixture.speciesName, { force: true });
        cy.get('select#strain-select').select(fixture.strainName, { force: true });
        cy.get('#create-species-with-strain').click();

        cy.wait('@createSpeciesWithStrain').its('response.statusCode').should('eq', 302);
        cy.wait('@reload').its('response.statusCode').should('eq', 200);

        cy.get('#species-with-strain-list').contains(fixture.speciesName)
          .parent()
          .contains(fixture.speciesWithStrainName);
        checkForSuccessToast();
      });
    });

    it('should add an alias to an existing species with strain', () => {
      cy.intercept('/speciesWithStrain/createImportAlias/*').as('createImportAlias');
      cy.fixture('speciesWithStrain').then((fixture) => {
        cy.get('#species-with-strain-list')
          .contains(fixture.speciesWithStrainName)
          .parent()
          .parent()
          .as('row');

        cy.get('@row').find('.edit-switch-label').click();
        cy.get('@row').find('input[name="importAlias"]').clear();
        cy.get('@row').find('input[name="importAlias"]').type(fixture.importAlias);
        cy.get('@row').find('button.save').click();

        cy.wait('@createImportAlias').its('response.statusCode').should('eq', 200);
        cy.get('@row').contains(fixture.importAlias);
      });
    });

    it('should set an species with strain to legacy and undo it again', () => {
      cy.intercept('/speciesWithStrain/changeLegacyState*').as('changeLegacyState');
      cy.get('#species-with-strain-list li li').eq(0).find('.legacy-container label').click();

      cy.wait('@changeLegacyState').its('response.statusCode').should('eq', 302);
      cy.wait('@reload').its('response.statusCode').should('eq', 200);

      cy.get('input.slider').should('have.attr', 'checked');
      checkForSuccessToast();

      cy.get('#species-with-strain-list li li').eq(0).find('.legacy-container label').click();
      cy.wait('@changeLegacyState').its('response.statusCode').should('eq', 302);
      cy.wait('@reload').its('response.statusCode').should('eq', 200);

      cy.get('input.slider').should('not.have.attr', 'checked');
      checkForSuccessToast();
    });
  });

  function checkForSuccessToast() {
    cy.get('div.otpSuccessToast').should('exist');
    cy.get('div.otpErrorToast').should('not.exist');
  }
});
