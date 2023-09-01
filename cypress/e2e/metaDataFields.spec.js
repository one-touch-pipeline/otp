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

describe('Check metadata data fields page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should edit a library preparation kits settings', () => {
      cy.intercept('/metaDataFields/libraryPreparationKits*').as('libraryPreparationKits');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Library Prep. Kit').click();
      const libPrepKit = 'Agilent SureSelect V3';
      cy.get('table#metadatafields-datatable tbody').contains(libPrepKit).parent('tr').find('td')
        .as('tableCells');

      // Create import alias
      const importAlias = Cypress._.random(0, 1e6);
      cy.wait('@libraryPreparationKits').then(() => {
        cy.intercept('/metaDataFields/createLibraryPreparationKitImportAlias/*').as('createImportAlias');

        cy.get('@tableCells').eq(2).as('importAliasCell');
        cy.get('@importAliasCell').find('button.add').click();
        cy.get('@importAliasCell').find('input[name=importAlias]').type(importAlias);
        cy.get('@importAliasCell').find('button.save').click();

        cy.wait('@createImportAlias').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // Edit adapter file
      const adapterFilePath = `/${Cypress._.random(0, 1e6)}/${Cypress._.random(0, 1e6)}`;
      cy.wait('@libraryPreparationKits').then(() => {
        cy.intercept('/metaDataFields/addAdapterFileToLibraryPreparationKit*').as('addAdaptFile');

        cy.get('@tableCells').eq(4).as('adaptFileCell');
        cy.get('@adaptFileCell').find('button.add').click();
        cy.get('@adaptFileCell').find('input[name=adapterFile]').type(adapterFilePath);
        cy.get('@adaptFileCell').find('button.save').click();

        cy.wait('@addAdaptFile').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // Edit adapter Sequence
      const adapterSequence = Cypress._.random(0, 1e6);
      cy.wait('@libraryPreparationKits').then(() => {
        cy.intercept('/metaDataFields/addAdapterSequenceToLibraryPreparationKit*').as('addAdapterSequence');

        cy.get('@tableCells').eq(6).as('adapterSequenceCell');
        cy.get('@adapterSequenceCell').find('button.add').click();
        cy.get('@adapterSequenceCell').find('input[name=reverseComplementAdapterSequence]').type(adapterSequence);
        cy.get('@adapterSequenceCell').find('button.save').click();

        cy.wait('@addAdapterSequence').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // Change legacy state
      cy.wait('@libraryPreparationKits').then(() => {
        cy.intercept('/metaDataFields/changeLibPrepKitLegacyState*').as('changeLegacyState');
        cy.get('@tableCells').eq(8).find('label').click();
        cy.wait('@changeLegacyState').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });
      });

      // Check all changed values
      cy.wait('@libraryPreparationKits').then(() => {
        cy.get('@tableCells').eq(1).contains(importAlias);
        cy.get('@tableCells').eq(3).contains(adapterFilePath);
        cy.get('@tableCells').eq(5).contains(adapterSequence);
        cy.get('@tableCells').eq(8).contains('true');
      });

      // Reset legacy state
      cy.intercept('/metaDataFields/changeLibPrepKitLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(8).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new library preparation kit', () => {
      cy.intercept('/metaDataFields/libraryPreparationKits*').as('libraryPreparationKits');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Library Prep. Kit').click();

      // Edit import alias
      const libPrepKit = Cypress._.random(0, 1e6);
      const adapterFile = `/${Cypress._.random(0, 1e6)}/${Cypress._.random(0, 1e6)}`;
      const adapterSequence = Cypress._.random(0, 1e6);

      cy.intercept('/metaDataFields/createLibraryPreparationKit*').as('createLibPrepKit');

      cy.get('div.new-lib-prep-kit-container').as('createLibPrepKitContainer');
      cy.get('@createLibPrepKitContainer').find('button.add').click();
      cy.get('@createLibPrepKitContainer').find('input[name=name]').type(libPrepKit);
      cy.get('@createLibPrepKitContainer').find('input[name=adapterFile]').type(adapterFile);
      cy.get('@createLibPrepKitContainer').find('input[name=reverseComplementAdapterSequence]').type(adapterSequence);
      cy.get('@createLibPrepKitContainer').find('button.save').click();

      cy.wait('@createLibPrepKit').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body).to.contain({ success: true });
      });

      cy.wait('@libraryPreparationKits').then(() => {
        cy.get('table#metadatafields-datatable tbody tr').contains(libPrepKit).parent('tr').find('td')
          .as('tableCells');
        cy.get('@tableCells').eq(3).contains(adapterFile);
        cy.get('@tableCells').eq(5).contains(adapterSequence);
        cy.get('@tableCells').eq(8).contains('false');
      });
    });

    it('should edit a antibody targets settings', () => {
      cy.intercept('/metaDataFields/antibodyTargets*').as('antibodyTargets');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Antibody Target').click();
      const antibodyTarget = 'ATAC';
      cy.get('table#metadatafields-datatable tbody tr').contains(antibodyTarget).parent('tr').find('td')
        .as('tableCells');

      // Edit import alias
      const importAlias = Cypress._.random(0, 1e6);
      cy.intercept('/metaDataFields/createAntibodyTargetImportAlias/*').as('createImportAlias');
      cy.wait('@antibodyTargets').then(() => {
        cy.get('@tableCells').eq(2).as('importAliasCell');
        cy.get('@importAliasCell').find('button.add').click();
        cy.get('@importAliasCell').find('input[name=importAlias]').type(importAlias);
        cy.get('@importAliasCell').find('button.save').click();

        cy.wait('@createImportAlias').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // Change legacy state
      cy.wait('@antibodyTargets').then(() => {
        cy.intercept('/metaDataFields/changeAntibodyTargetLegacyState*').as('changeLegacyState');
        cy.get('@tableCells').eq(3).find('label').click();
        cy.wait('@changeLegacyState').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });
      });

      // Check all changed values
      cy.wait('@antibodyTargets').then(() => {
        cy.get('@tableCells').eq(1).contains(importAlias);
        cy.get('@tableCells').eq(3).contains('true');
      });

      // Reset legacy state
      cy.intercept('/metaDataFields/changeAntibodyTargetLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(3).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new antibody target', () => {
      cy.intercept('/metaDataFields/antibodyTargets*').as('antibodyTargets');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Antibody Target').click();

      const antibodyTargetName = Cypress._.random(0, 1e6);

      cy.intercept('/metaDataFields/createAntibodyTarget*').as('createAntibodyTarget');

      cy.get('div.new-antibody-target-container').as('createAntibodyTargetContainer');
      cy.get('@createAntibodyTargetContainer').find('button.add').click();
      cy.get('@createAntibodyTargetContainer').find('input[name=name]').type(antibodyTargetName);
      cy.get('@createAntibodyTargetContainer').find('button.save').click();

      cy.wait('@createAntibodyTarget').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body).to.contain({ success: true });
      });

      cy.get('table#metadatafields-datatable tbody tr').as('tableRows');

      cy.wait('@antibodyTargets').then(() => {
        cy.get('@tableRows').contains(antibodyTargetName);
      });
    });

    it('should set seq center to legacy', () => {
      cy.intercept('/metaDataFields/seqCenters*').as('seqCenters');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Seq. Center').click();

      const seqCenter = 'ExampleCenter';
      cy.get('table#metadatafields-datatable tbody tr').contains(seqCenter).parent('tr').find('td')
        .as('tableCells');

      cy.intercept('/metaDataFields/changeSeqCenterLegacyState*').as('changeLegacyState');

      cy.get('@tableCells').eq(4).find('label').click();

      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });

      cy.wait('@seqCenters').then(() => {
        cy.get('@tableCells').eq(4).find('span').contains('true');
      });

      // Reset legacy state
      cy.intercept('/metaDataFields/changeSeqCenterLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(4).find('label').click();

      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new seq center', () => {
      cy.intercept('/metaDataFields/seqCenters*').as('seqCenters');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Seq. Center').click();

      const seqCenterName = Cypress._.random(0, 1e6);
      const directoryName = Cypress._.random(0, 1e6);

      cy.intercept('/metaDataFields/createSeqCenter*').as('createSeqCenter');

      cy.get('div.new-seq-center-container').as('createSeqCenterContainer');
      cy.get('@createSeqCenterContainer').find('button.add').click();
      cy.get('@createSeqCenterContainer').find('input[name=name]').type(seqCenterName);
      cy.get('@createSeqCenterContainer').find('input[name=dirName]').type(directoryName);
      cy.get('@createSeqCenterContainer').find('button.save').click();

      cy.wait('@createSeqCenter').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body).to.contain({ success: true });
      });

      cy.get('table#metadatafields-datatable tbody tr').as('tableRows');

      cy.wait('@seqCenters').then(() => {
        cy.get('table#metadatafields-datatable tbody tr').contains(seqCenterName).parent('tr').find('td')
          .as('tableCells');
        cy.get('@tableCells').eq(1).contains(directoryName);
        cy.get('@tableCells').eq(2).contains('-');
        cy.get('@tableCells').eq(3).contains('false');
        cy.get('@tableCells').eq(4).contains('false');
      });
    });

    it('should edit a seq platforms settings', () => {
      cy.intercept('/metaDataFields/seqPlatforms*').as('seqPlatforms');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Seq. Platforms').click();

      const seqPlatform = 'ExampleSeqPlatform';
      cy.get('table#metadatafields-datatable tbody tr').contains(seqPlatform).parent('tr').find('td')
        .as('tableCells');

      // create model import alias
      const modelImportAlias = Cypress._.random(0, 1e6);
      cy.wait('@seqPlatforms').then(() => {
        cy.intercept('/metaDataFields/createSeqPlatformModelLabelImportAlias/*').as('createModelImportAlias');

        cy.get('@tableCells').eq(3).as('importModelAliasCell');
        cy.get('@importModelAliasCell').find('button.add').click();
        cy.get('@importModelAliasCell').find('input[name=importAlias]').type(modelImportAlias);
        cy.get('@importModelAliasCell').find('button.save').click();

        cy.wait('@createModelImportAlias').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // create SeqKitImportAlias
      const seqKitImportAlias = Cypress._.random(0, 1e6);
      cy.wait('@seqPlatforms').then(() => {
        cy.intercept('/metaDataFields/createSequencingKitLabelImportAlias/*').as('createModelImportAlias');

        cy.get('@tableCells').eq(6).as('importModelAliasCell');
        cy.get('@importModelAliasCell').find('button.add').click();
        cy.get('@importModelAliasCell').find('input[name=importAlias]').type(seqKitImportAlias);
        cy.get('@importModelAliasCell').find('button.save').click();

        cy.wait('@createModelImportAlias').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // Change legacy state
      cy.wait('@seqPlatforms').then(() => {
        cy.intercept('/metaDataFields/changeSeqPlatformLegacyState*').as('changeLegacyState');
        cy.get('@tableCells').eq(7).find('label').click();
        cy.wait('@changeLegacyState').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });
      });

      // Check all changed values
      cy.wait('@seqPlatforms').then(() => {
        cy.get('@tableCells').eq(2).contains(modelImportAlias);
        cy.get('@tableCells').eq(5).contains(seqKitImportAlias);
        cy.get('@tableCells').eq(7).contains('true');
      });

      // Reset legacy state
      cy.intercept('/metaDataFields/changeSeqPlatformLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(7).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new seq platform', () => {
      cy.intercept('/metaDataFields/seqPlatforms*').as('seqPlatforms');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Seq. Platforms').click();

      const seqPlatformName = Cypress._.random(0, 1e6);
      const modelName = Cypress._.random(0, 1e6);
      const kitName = Cypress._.random(0, 1e6);

      cy.intercept('/metaDataFields/createSeqPlatform*').as('createSeqPlatform');

      cy.get('div.new-seq-platform-container').as('createSeqPlatformContainer');
      cy.get('@createSeqPlatformContainer').find('button.add').click();
      cy.get('@createSeqPlatformContainer').find('input[name=platform]').type(seqPlatformName);
      cy.get('@createSeqPlatformContainer').find('input[name=model]').type(modelName);
      cy.get('@createSeqPlatformContainer').find('input[name=kit]').type(kitName);
      cy.get('@createSeqPlatformContainer').find('button.save').click();

      cy.wait('@createSeqPlatform').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body).to.contain({ success: true });
      });

      cy.get('table#metadatafields-datatable tbody tr').as('tableRows');

      cy.wait('@seqPlatforms').then(() => {
        cy.get('table#metadatafields-datatable tbody tr').contains(seqPlatformName).parent('tr').find('td')
          .as('tableCells');
        cy.get('@tableCells').eq(1).contains(modelName);
        cy.get('@tableCells').eq(4).contains(kitName);
        cy.get('@tableCells').eq(7).contains('false');
      });
    });

    it('should edit a seq types settings', () => {
      cy.intercept('/metaDataFields/seqTypes*').as('seqTypes');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Seq. Types').click();

      const seqType = 'EXON';
      cy.get('table#metadatafields-datatable tbody tr').contains(seqType).parent('tr').find('td')
        .as('tableCells');

      // Toggle needs bed file setting
      cy.wait('@seqTypes').then(() => {
        cy.intercept('/metaDataFields/toggleNeedsBedFile*').as('toggleNeedsBedFile');

        cy.get('@tableCells').eq(3).as('needsBedFileCell');
        cy.get('@needsBedFileCell').find('button.edit').click();
        cy.get('@needsBedFileCell').find('button.toggle').click();

        cy.wait('@toggleNeedsBedFile').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // Add sequencing read Types
      cy.intercept('/metaDataFields/createLayout/*').as('createLayout');

      cy.get('@tableCells').eq(6).as('addLibraryLayoutCell');
      cy.get('@addLibraryLayoutCell').find('button.add').click();
      cy.get('@addLibraryLayoutCell').find('input#mate_pair').check();
      cy.get('@addLibraryLayoutCell').find('input#single').check();
      cy.get('@addLibraryLayoutCell').find('button.save').click();

      cy.wait('@createLayout').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body).to.contain({ success: true });
      });

      // create import alias
      const importAlias = Cypress._.random(0, 1e6);
      cy.wait('@seqTypes').then(() => {
        cy.intercept('/metaDataFields/createSeqTypeImportAlias/*').as('createImportAlias');

        cy.get('@tableCells').eq(9).as('importAliasCell');
        cy.get('@importAliasCell').find('button.add').click();
        cy.get('@importAliasCell').find('input[name=importAlias]').type(importAlias);
        cy.get('@importAliasCell').find('button.save').click();

        cy.wait('@createImportAlias').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body).to.contain({ success: true });
        });
      });

      // Change legacy state
      cy.wait('@seqTypes').then(() => {
        cy.intercept('/metaDataFields/changeSeqTypeLegacyState*').as('changeLegacyState');
        cy.get('@tableCells').eq(10).find('label').click();
        cy.wait('@changeLegacyState').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });
      });

      // Check all changes
      cy.wait('@seqTypes').then(() => {
        cy.get('@tableCells').eq(1).contains('false');
        cy.get('@tableCells').eq(2).contains('false');
        cy.get('@tableCells').eq(3).contains('false');
        cy.get('@tableCells').eq(5).contains('SINGLE; PAIRED; MATE_PAIR');
        cy.get('@tableCells').eq(8).contains(importAlias);
        cy.get('@tableCells').eq(10).contains('true');
      });

      // Reset legacy status
      cy.intercept('/metaDataFields/changeSeqTypeLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(10).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new seq type', () => {
      cy.intercept('/metaDataFields/seqTypes*').as('seqTypes');
      cy.visit('/metaDataFields');
      cy.get('.tab-menu a').contains('Seq. Types').click();

      const seqTypeName = Cypress._.random(0, 1e6);
      const importAlias = Cypress._.random(0, 1e6);
      const displayName = Cypress._.random(0, 1e6);
      const dirName = Cypress._.random(0, 1e6);

      cy.intercept('/metaDataFields/createSeqType*').as('createSeqType');

      cy.get('div#newSeqTypeContainer').as('newSeqTypeContainer');
      cy.get('@newSeqTypeContainer').find('input[name=seqTypeName]').type(seqTypeName);
      cy.get('@newSeqTypeContainer').find('input[name=aliases]').type(importAlias);
      cy.get('@newSeqTypeContainer').find('input[name=displayName]').type(displayName);
      cy.get('@newSeqTypeContainer').find('input[name=dirName]').type(dirName);
      cy.get('@newSeqTypeContainer').find('input[name=singleCell]').check();
      cy.get('@newSeqTypeContainer').find('input[name=needsBedFile]').check();
      cy.get('@newSeqTypeContainer').find('input[name=single]').check();
      cy.get('@newSeqTypeContainer').find('input[name=Create]').click();

      cy.wait('@createSeqType').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });

      cy.wait('@seqTypes').then(() => {
        cy.get('table#metadatafields-datatable tbody tr').contains(seqTypeName).parent('tr').find('td')
          .as('tableCells');
        cy.get('@tableCells').eq(1).contains('true');
        cy.get('@tableCells').eq(2).contains('false');
        cy.get('@tableCells').eq(3).contains('true');
        cy.get('@tableCells').eq(4).contains(dirName);
        cy.get('@tableCells').eq(5).contains('SINGLE; PAIRED');
        cy.get('@tableCells').eq(7).contains(displayName);
        cy.get('@tableCells').eq(8).contains(importAlias);
        cy.get('@tableCells').eq(10).contains('false');
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/metaDataFields');
      cy.checkAccessDenied('/metaDataFields/libraryPreparationKits');
      cy.checkAccessDenied('/metaDataFields/createLibraryPreparationKitImportAlias');
      cy.checkAccessDenied('/metaDataFields/addAdapterFileToLibraryPreparationKit');
      cy.checkAccessDenied('/metaDataFields/addAdapterSequenceToLibraryPreparationKit');
      cy.checkAccessDenied('/metaDataFields/changeLibPrepKitLegacyState');
      cy.checkAccessDenied('/metaDataFields/createLibraryPreparationKit');
      cy.checkAccessDenied('/metaDataFields/antibodyTargets');
      cy.checkAccessDenied('/metaDataFields/createAntibodyTargetImportAlias');
      cy.checkAccessDenied('/metaDataFields/changeAntibodyTargetLegacyState');
      cy.checkAccessDenied('/metaDataFields/createAntibodyTarget');
      cy.checkAccessDenied('/metaDataFields/seqCenters');
      cy.checkAccessDenied('/metaDataFields/changeSeqCenterLegacyState');
      cy.checkAccessDenied('/metaDataFields/createSeqCenter');
      cy.checkAccessDenied('/metaDataFields/seqPlatforms');
      cy.checkAccessDenied('/metaDataFields/createSeqPlatformModelLabelImportAlias');
      cy.checkAccessDenied('/metaDataFields/changeSeqPlatformLegacyState');
      cy.checkAccessDenied('/metaDataFields/createSeqPlatform');
      cy.checkAccessDenied('/metaDataFields/toggleNeedsBedFile');
      cy.checkAccessDenied('/metaDataFields/createLayout');
      cy.checkAccessDenied('/metaDataFields/createSeqTypeImportAlias');
      cy.checkAccessDenied('/metaDataFields/changeSeqTypeLegacyState');
      cy.checkAccessDenied('/metaDataFields/createSeqType');
    });
  });
});
