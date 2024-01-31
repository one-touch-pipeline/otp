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

describe('test for individual page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should filter the table by project and identifier', () => {
      cy.intercept('/individual/dataTableSource*').as('loadDataTable');
      cy.visit('/individual/list');

      cy.fixture('individual.json').then((fixture) => {
        cy.get('#searchCriteriaTable td.attribute select').select('Project', { force: true });
        cy.get('#searchCriteriaTable select[name=projectSelection]').select(fixture.filterProject, { force: true });
        cy.get('#searchCriteriaTable td.add input[type=button]').click();

        cy.get('#searchCriteriaTable tr').eq(1).find('td.attribute select').select('Identifier', { force: true });
        cy.get('#searchCriteriaTable tr').eq(1).find('input[name=pidSearch]').type(fixture.filterIdentifier);

        // Every character typed in pid search is sending one backend call.
        cy.wait('@loadDataTable').wait('@loadDataTable').wait('@loadDataTable')
          .wait('@loadDataTable')
          .wait('@loadDataTable')
          .wait('@loadDataTable')
          .wait('@loadDataTable')
          .wait('@loadDataTable')
          .wait('@loadDataTable')
          .wait('@loadDataTable')
          .then((interception) => {
            expect(interception.response.statusCode).to.equal(200);
            expect(interception.response.body.aaData).to.have.length(2);
          });

        cy.get('table#individualTable tbody').find('tr').should('have.length', 2).each((tableRow) => {
          cy.wrap(tableRow).find('td').eq(0).should('contain', fixture.filterIdentifier);
          cy.wrap(tableRow).find('td').eq(1).contains(fixture.filterProject)
            .should('exist');
        });
      });
    });

    it('should show sensible and editable data when visiting show page', () => {
      cy.fixture('individual.json').then((fixture) => {
        cy.visit(`/individual/show/${fixture.individualId}`);

        cy.get(fixture.editSampleTableId).should('exist');
        cy.get(fixture.UUIDEntrySelector).should('exist');
        cy.get(fixture.individualEditTypeButtonSelector).should('exist');
      });
    });

    it('should add two sample names, edit these and remove them', () => {
      cy.fixture('individual.json').then((fixture) => {
        cy.intercept(`/individual/editNewSampleIdentifier/${fixture.individualId}*`).as('editSampleIdentifier');
        cy.visit(`/individual/show/${fixture.individualId}`);

        const sampleIdentifier1 = 'test123';
        const sampleIdentifier2 = 'djs_32%4-';
        const sampleIdentifier2newName = 'abc&-';

        cy.get('#individualSampleTbl tbody tr').first().as('sampleContainer');
        cy.get('@sampleContainer').find('.expandable-button').click();

        cy.get('@sampleContainer').find('button.add-field').click();
        cy.get('@sampleContainer').find('input#newIdentifiersNames').eq(0).type(sampleIdentifier1);
        cy.get('@sampleContainer').find('input#newIdentifiersNames').eq(1).type(sampleIdentifier2);
        cy.get('@sampleContainer').find('input#update').click();

        cy.get('@sampleContainer').contains(new RegExp(sampleIdentifier1, 'g')).should('exist');
        cy.get('@sampleContainer').contains(new RegExp(sampleIdentifier2, 'g')).should('exist');

        // Delete one sample Identifier and edit other one
        cy.get('@sampleContainer').find('.expandable-button').click();

        cy.get('@sampleContainer').find('input[name="editedSampleIdentifiers[0].delete"]').click();
        cy.get('@sampleContainer').find('input[name="editedSampleIdentifiers[1].name"]').clear()
          .type(sampleIdentifier2newName);
        cy.get('@sampleContainer').find('input#update').click();

        cy.get('#individualSampleTbl').contains(new RegExp(sampleIdentifier2newName, 'g')).should('exist');
        cy.get('#individualSampleTbl').contains(new RegExp(sampleIdentifier1, 'g')).should('not.exist');
        cy.get('#individualSampleTbl').contains(new RegExp(sampleIdentifier2, 'g')).should('not.exist');

        // Delete last sample Identifier
        cy.get('@sampleContainer').find('.expandable-button').first().click();
        cy.get('@sampleContainer').find('input[name="editedSampleIdentifiers[0].delete"]').click();
        cy.get('@sampleContainer').find('input#update').click();

        cy.get('#individualSampleTbl').contains(sampleIdentifier2newName).should('not.exist');
        cy.get('#individualSampleTbl').contains(sampleIdentifier1).should('not.exist');
        cy.get('#individualSampleTbl').contains(sampleIdentifier2).should('not.exist');
      });
    });

    it('should add a new sample type', () => {
      cy.fixture('individual.json').then((fixture) => {
        cy.intercept(`/individual/updateField/${fixture.individualId}*`).as('updateField');
        cy.visit(`/individual/show/${fixture.individualId}`);

        cy.get('#individualSampleTbl .new-sample-type button.insert').click();
        cy.get('#individualSampleTbl .new-sample-type [role="textbox"]').then((textbox) => {
          const addedSampleType = textbox.text();
          cy.get('#individualSampleTbl .new-sample-type button.save').click();

          cy.get('#individualSampleTbl').contains(addedSampleType).should('exist');
        });
      });
    });

    it('should edit type of individual', () => {
      cy.fixture('individual.json').then((fixture) => {
        cy.intercept(`/individual/updateField/${fixture.individualId}*`).as('updateField');
        cy.visit(`/individual/show/${fixture.individualId}`);

        cy.get(fixture.individualEditTypeButtonSelector).click();
        cy.get('.typeDropDown select').as('typeSelect').then((select) => {
          const newType = 'POOL';
          const oldType = select.val();

          cy.get('@typeSelect').select(newType, { force: true });
          cy.get('.typeDropDown button.save').click();

          cy.wait('@updateField').then((interception) => {
            expect(interception.response.statusCode).to.equal(200);
          });

          // revisit page to check, if update was successfully and change value back.
          cy.visit(`/individual/show/${fixture.individualId}`);

          cy.get('.typeDropDown').contains(newType).should('exist');

          cy.get(fixture.individualEditTypeButtonSelector).click();
          cy.get('.typeDropDown select').select(oldType, { force: true });
          cy.get('.typeDropDown button.save').click();

          cy.wait('@updateField').then((interception) => {
            expect(interception.response.statusCode).to.equal(200);
          });
        });
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should load data table on initialization and download csv file, when button is clicked', () => {
      cy.intercept('/individual/dataTableSource*').as('loadDataTable');

      cy.visit('/individual/list');

      cy.wait('@loadDataTable').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.get('table#individualTable tbody tr').should('have.length', 3);
      cy.get('table#individualTable tbody').find('tr').each((tableRow) => {
        cy.wrap(tableRow).find('td').eq(1).contains('ExampleProject')
          .should('exist');
      });

      // Download Csv file via Button
      cy.get('button.buttons-csv').click();

      cy.checkDownloadByContent('List_of_Individuals', '.csv', [
        'Identifier', 'Project', 'Type'
      ]);
    });

    it('should filter by project and individual, when quick search is used', () => {
      cy.intercept('/individual/dataTableSource*').as('loadDataTable');
      cy.visit('/individual/list');

      cy.fixture('individual.json').then((fixture) => {
        cy.get('#searchbox input').type(fixture.filterQuickSearch);

        cy.wait('@loadDataTable').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('table#individualTable tbody tr').should('have.length', 1);
        cy.get('table#individualTable tbody').find('tr').each((tableRow) => {
          cy.wrap(tableRow).find('td').eq(0).should('contain', fixture.filterQuickSearch.toLowerCase());
        });
      });
    });

    it('should redirect to show page when individual is clicked', () => {
      cy.intercept('/individual/dataTableSource*').as('loadDataTable');
      cy.visit('/individual/list');

      cy.wait('@loadDataTable');

      cy.get('#individualTable tbody tr').first().find('td a').eq(0)
        .click();
      cy.checkPage('/individual/show');
    });

    it('should hide all editing buttons when visiting show page', () => {
      cy.fixture('individual.json').then((fixture) => {
        cy.visit(`/individual/show/${fixture.individualId}`);

        cy.get(fixture.editSampleTableId).should('not.exist');
        cy.get(fixture.UUIDEntrySelector).should('not.exist');
        cy.get(fixture.individualEditTypeButtonSelector).should('not.exist');

        cy.get('#lane-overview > :nth-child(2n + 1)').should('have.length', 6)
          .should('have.prop', 'tagName')
          .should('eq', 'H3');
        cy.get('#lane-overview > :nth-child(2n)').should('have.length', 6)
          .should('have.prop', 'tagName')
          .should('eq', 'TABLE');
      });
    });
  });
});
