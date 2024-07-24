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

describe('Check cell ranger configuration page', () => {
  'use strict';

  context('when user is part of a project', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should visit the index page', () => {
      cy.visit('/cellRangerConfiguration/index');
    });

    it('should select cell ranger version and save it', () => {
      cy.intercept('/cellRangerConfiguration/updateVersion*').as('updateVersion');
      cy.fixture('cellRangerConfiguration.json').then((fixture) => {
        cy.get('select#versionSelect').select(fixture.versionToSelect, { force: true });
        cy.get('input#submit').click();

        cy.wait('@updateVersion').then((interception) => {
          expect(interception.response.statusCode).to.equal(302);
        });

        cy.get('div.annotation-box').should('not.contain.text', 'Please configure the Cell Ranger version to use');
        cy.get('input[type=submit]#save').should('exist');
        cy.get('button#executeButton').should('exist');
        cy.get('div#otpToastBox').contains('stored successfully');
        cy.get('select#versionSelect').should('have.value', fixture.versionToSelect);
      });
    });

    it('should select cell ranger version, then activate and deactivate automatic running', () => {
      cy.visit('/cellRangerConfiguration/index');

      cy.intercept('/cellRangerConfiguration/updateAutomaticExecution*').as('updateAutomaticExecution');

      cy.fixture('cellRangerConfiguration.json').then((fixture) => {
        cy.get('#updateAutomaticExecution #enableAutoExec').click({ force: true });
        cy.get('#updateAutomaticExecution #referenceGenomeIndex2')
          .select(fixture.referenceGenome, { force: true });
        cy.get('input#save').click();

        cy.wait('@updateAutomaticExecution').then((interception) => {
          expect(interception.response.statusCode).to.equal(302);
        });

        cy.get('#updateAutomaticExecution #enableAutoExec').should('be.checked');

        cy.get('#updateAutomaticExecution #enableAutoExec').click({ force: true });
        cy.get('input#save').click();

        cy.wait('@updateAutomaticExecution').then((interception) => {
          expect(interception.response.statusCode).to.equal(302);
        });

        cy.get('#updateAutomaticExecution #enableAutoExec').should('not.be.checked');
      });
    });

    it('should search for samples to process and execute cell ranger all except one of them', () => {
      cy.intercept('/cellRangerConfiguration/createMwp*').as('createMwp');
      cy.intercept('/cellRangerConfiguration/getIndividualsAndSampleTypesBySeqType*').as('getSamples');
      const expectedCells = Math.floor(Math.random() * 3000);
      cy.fixture('cellRangerConfiguration.json').then((fixture) => {
        cy.get('select#referenceGenomeSelect').select(fixture.referenceGenome, { force: true });
        cy.get('select#individualSelect').select(fixture.individual, { force: true });
        cy.wait('@getSamples').then((interception) => {
          expect(interception.response.statusCode).to.equal(200);
        });

        cy.get('input#expectedCellsRadio2').click();
        cy.get('form#sampleForm input[name="expectedCellsValue"]').type(expectedCells);

        cy.get('table#sampleTable tbody tr').eq(1).find('input.tableCheckbox').click();
        cy.get('table#mwpTable tbody tr').then((mwpTableRowsBefore) => {
          cy.get('table#sampleTable tbody tr').then((sampleTableRows) => {
            const cellRunsBefore = mwpTableRowsBefore.length;
            const samplesSubmitted = sampleTableRows.length - 1;

            cy.get('button#executeButton').click();

            cy.wait('@createMwp').then((interception) => {
              expect(interception.response.statusCode).to.equal(200);
              const amountOfSamplesSend = (interception.request.body.match(/samples/g) || []).length;
              expect(amountOfSamplesSend).to.equal(samplesSubmitted);
            });

            /** The created mwps should be contained in the table for the existing runs */
            cy.get('#mwpTable_processing').should('not.be.visible');
            cy.get('.otpSuccessToast').should('be.visible').and('contain.text', 'successfully created');
            cy.get('table#mwpTable tbody tr').then((mwpTableRowsAfter) => {
              expect(mwpTableRowsAfter.length).to.eq(cellRunsBefore + samplesSubmitted);
            });
          });
        });
      });
    });

    it('should not be able to invalidate the selected cell ranger version', () => {
      cy.intercept('/cellRangerConfiguration/updateVersion*').as('updateVersion');

      cy.fixture('cellRangerConfiguration.json').then((fixture) => {
        cy.get('select#versionSelect').select(0, { force: true });
        cy.get('input#submit').click();

        cy.wait('@updateVersion').then((interception) => {
          expect(interception.response.statusCode).to.equal(302);
        });

        cy.get('div.annotation-box').should('not.contain.text', 'Please configure the Cell Ranger version to use');
        cy.get('input[type=submit]#save').should('exist');
        cy.get('button#executeButton').should('exist');
        cy.get('div#otpToastBox').contains('could not be stored');
        cy.get('select#versionSelect').should('have.value', fixture.versionToSelect);
      });
    });
  });

  context('when user is a operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should be able to visit the page', () => {
      cy.visit('/cellRangerConfiguration/index');
    });

    it('should invalidate the selected cell ranger version', () => {
      cy.intercept('/configurePipeline/invalidateConfig*').as('invalidateConfig');
      cy.fixture('cellRangerConfiguration.json').then((fixture) => {
        cy.get('input#invalidateConfig').click();
        cy.wait('@invalidateConfig').then((interception) => {
          expect(interception.response.statusCode).to.equal(302);
        });

        cy.get('div.annotation-box').should('contain.text', 'Please configure the Cell Ranger version to use');
        cy.get('input[type=submit]#save').should('not.exist');
        cy.get('button#executeButton').should('not.exist');
        cy.get('div#otpToastBox').contains('invalidated successfully');
        cy.get('select#versionSelect').should('not.have.value', fixture.versionToSelect);
      });
    });
  });

  context('when user is not part of a project', () => {
    beforeEach(() => {
      cy.loginAs('departmentHead');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/cellRangerConfiguration/index', 404);
    });
  });
});
