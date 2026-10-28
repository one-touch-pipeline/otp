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

describe('Check metadata data fields page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should edit a library preparation kits settings', () => {
      cy.visit('/metaDataFields');
      openTab('Library Prep. Kit', 'libraryPreparationKits');
      const libPrepKit = 'Agilent SureSelect V3';
      cy.get('table#metadatafields-datatable tbody').contains(libPrepKit).parent('tr').find('td')
        .as('tableCells');

      // Create import alias
      const importAlias = Cypress._.random(0, 1e6);
      cy.intercept('/metaDataFields/createLibraryPreparationKitImportAlias/*').as('createImportAlias');

      cy.get('@tableCells').eq(2).as('importAliasCell');
      cy.get('@importAliasCell').find('button.add').click();
      cy.get('@importAliasCell').find('input[name=importAlias]').type(importAlias);
      cy.get('@importAliasCell').find('button.save').click();

      cy.wait('@createImportAlias').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      waitForReloadShowing('@tableCells', 1, importAlias);

      // Edit adapter file
      const adapterFilePath = `/${Cypress._.random(0, 1e6)}/${Cypress._.random(0, 1e6)}`;
      cy.intercept('/metaDataFields/addAdapterFileToLibraryPreparationKit*').as('addAdaptFile');

      cy.get('@tableCells').eq(4).as('adaptFileCell');
      cy.get('@adaptFileCell').find('button.add').click();
      cy.get('@adaptFileCell').find('input[name=adapterFile]').type(adapterFilePath);
      cy.get('@adaptFileCell').find('button.save').click();

      cy.wait('@addAdaptFile').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      waitForReloadShowing('@tableCells', 3, adapterFilePath);

      // Edit adapter Sequence
      const adapterSequence = Cypress._.random(0, 1e6);
      cy.intercept('/metaDataFields/addAdapterSequenceToLibraryPreparationKit*').as('addAdapterSequence');

      cy.get('@tableCells').eq(6).as('adapterSequenceCell');
      cy.get('@adapterSequenceCell').find('button.add').click();
      cy.get('@adapterSequenceCell').find('input[name=reverseComplementAdapterSequence]').type(adapterSequence);
      cy.get('@adapterSequenceCell').find('button.save').click();

      cy.wait('@addAdapterSequence').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      waitForReloadShowing('@tableCells', 5, adapterSequence);

      // Change legacy state
      cy.intercept('/metaDataFields/changeLibPrepKitLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(8).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
      waitForReloadShowing('@tableCells', 8, 'true');

      // Check all changed values
      cy.get('@tableCells').eq(1).contains(importAlias);
      cy.get('@tableCells').eq(3).contains(adapterFilePath);
      cy.get('@tableCells').eq(5).contains(adapterSequence);
      cy.get('@tableCells').eq(8).contains('true');

      // Reset legacy state
      cy.intercept('/metaDataFields/changeLibPrepKitLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(8).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new library preparation kit', () => {
      cy.visit('/metaDataFields');
      openTab('Library Prep. Kit', 'libraryPreparationKits');

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
      });

      // the page is reloaded after saving, the new kit is only shown on the reloaded page
      cy.get('table#metadatafields-datatable tbody tr').contains(libPrepKit).parent('tr').find('td')
        .as('tableCells');
      cy.get('@tableCells').eq(3).contains(adapterFile);
      cy.get('@tableCells').eq(5).contains(adapterSequence);
      cy.get('@tableCells').eq(8).contains('false');
    });

    it('should edit a antibody targets settings', () => {
      cy.visit('/metaDataFields');
      openTab('Antibody Target', 'antibodyTargets');
      const antibodyTarget = 'ATAC';
      cy.get('table#metadatafields-datatable tbody tr').contains(antibodyTarget).parent('tr').find('td')
        .as('tableCells');

      // Edit import alias
      const importAlias = Cypress._.random(0, 1e6);
      cy.intercept('/metaDataFields/createAntibodyTargetImportAlias/*').as('createImportAlias');
      cy.get('@tableCells').eq(2).as('importAliasCell');
      cy.get('@importAliasCell').find('button.add').click();
      cy.get('@importAliasCell').find('input[name=importAlias]').type(importAlias);
      cy.get('@importAliasCell').find('button.save').click();

      cy.wait('@createImportAlias').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      waitForReloadShowing('@tableCells', 1, importAlias);

      // Change legacy state
      cy.intercept('/metaDataFields/changeAntibodyTargetLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(3).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
      waitForReloadShowing('@tableCells', 3, 'true');

      // Check all changed values
      cy.get('@tableCells').eq(1).contains(importAlias);
      cy.get('@tableCells').eq(3).contains('true');

      // Reset legacy state
      cy.intercept('/metaDataFields/changeAntibodyTargetLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(3).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new antibody target', () => {
      cy.visit('/metaDataFields');
      openTab('Antibody Target', 'antibodyTargets');

      const antibodyTargetName = Cypress._.random(0, 1e6);

      cy.intercept('/metaDataFields/createAntibodyTarget*').as('createAntibodyTarget');

      cy.get('div.new-antibody-target-container').as('createAntibodyTargetContainer');
      cy.get('@createAntibodyTargetContainer').find('button.add').click();
      cy.get('@createAntibodyTargetContainer').find('input[name=name]').type(antibodyTargetName);
      cy.get('@createAntibodyTargetContainer').find('button.save').click();

      cy.wait('@createAntibodyTarget').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      // the page is reloaded after saving, the new antibodyTarget is only shown on the reloaded page
      cy.get('table#metadatafields-datatable tbody tr').contains(antibodyTargetName);
    });

    it('should set seq center to legacy', () => {
      cy.visit('/metaDataFields');
      openTab('Seq. Center', 'seqCenters');

      const seqCenter = 'ExampleCenter';
      cy.get('table#metadatafields-datatable tbody tr').contains(seqCenter).parent('tr').find('td')
        .as('tableCells');

      cy.intercept('/metaDataFields/changeSeqCenterLegacyState*').as('changeLegacyState');

      cy.get('@tableCells').eq(4).find('label').click();

      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });

      // the page is reloaded after the change, the page shown before still has the old state
      cy.get('@tableCells').eq(4).find('span').contains('true');

      // Reset legacy state
      cy.intercept('/metaDataFields/changeSeqCenterLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(4).find('label').click();

      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new seq center', () => {
      cy.visit('/metaDataFields');
      openTab('Seq. Center', 'seqCenters');

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
      });

      // the page is reloaded after saving, the new seq center is only shown on the reloaded page
      cy.get('table#metadatafields-datatable tbody tr').contains(seqCenterName).parent('tr').find('td')
        .as('tableCells');
      cy.get('@tableCells').eq(1).contains(directoryName);
      cy.get('@tableCells').eq(2).contains('-');
      cy.get('@tableCells').eq(3).contains('false');
      cy.get('@tableCells').eq(4).contains('false');
    });

    it('should edit a seq platforms settings', () => {
      cy.visit('/metaDataFields');
      openTab('Seq. Platforms', 'seqPlatforms');

      const seqPlatform = 'ExampleSeqPlatform';
      cy.get('table#metadatafields-datatable tbody tr').contains(seqPlatform).parent('tr').find('td')
        .as('tableCells');

      // create model import alias
      const modelImportAlias = Cypress._.random(0, 1e6);
      cy.intercept('/metaDataFields/createSeqPlatformModelLabelImportAlias/*').as('createModelImportAlias');

      cy.get('@tableCells').eq(3).as('importModelAliasCell');
      cy.get('@importModelAliasCell').find('button.add').click();
      cy.get('@importModelAliasCell').find('input[name=importAlias]').type(modelImportAlias);
      cy.get('@importModelAliasCell').find('button.save').click();

      cy.wait('@createModelImportAlias').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      waitForReloadShowing('@tableCells', 2, modelImportAlias);

      // create SeqKitImportAlias
      const seqKitImportAlias = Cypress._.random(0, 1e6);
      cy.intercept('/metaDataFields/createSequencingKitLabelImportAlias/*').as('createModelImportAlias');

      cy.get('@tableCells').eq(6).as('importModelAliasCell');
      cy.get('@importModelAliasCell').find('button.add').click();
      cy.get('@importModelAliasCell').find('input[name=importAlias]').type(seqKitImportAlias);
      cy.get('@importModelAliasCell').find('button.save').click();

      cy.wait('@createModelImportAlias').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      waitForReloadShowing('@tableCells', 5, seqKitImportAlias);

      // Change legacy state
      cy.intercept('/metaDataFields/changeSeqPlatformLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(7).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
      waitForReloadShowing('@tableCells', 7, 'true');

      // Check all changed values
      cy.get('@tableCells').eq(2).contains(modelImportAlias);
      cy.get('@tableCells').eq(5).contains(seqKitImportAlias);
      cy.get('@tableCells').eq(7).contains('true');

      // Reset legacy state
      cy.intercept('/metaDataFields/changeSeqPlatformLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(7).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new seq platform', () => {
      cy.visit('/metaDataFields');
      openTab('Seq. Platforms', 'seqPlatforms');

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
      });

      // the page is reloaded after saving, the new seq platform is only shown on the reloaded page
      cy.get('table#metadatafields-datatable tbody tr').contains(seqPlatformName).parent('tr').find('td')
        .as('tableCells');
      cy.get('@tableCells').eq(1).contains(modelName);
      cy.get('@tableCells').eq(4).contains(kitName);
      cy.get('@tableCells').eq(7).contains('false');
    });

    it('should edit a seq types settings', () => {
      cy.visit('/metaDataFields');
      openTab('Seq. Types', 'seqTypes');

      const seqType = 'EXON';
      cy.get('table#metadatafields-datatable tbody tr').contains(seqType).parent('tr').find('td')
        .as('tableCells');

      // Toggle the 'needs bed file' setting, this does not reload the page
      cy.intercept('/metaDataFields/toggleNeedsBedFile*').as('toggleNeedsBedFile');

      cy.get('@tableCells').eq(3).as('needsBedFileCell');
      cy.get('@needsBedFileCell').find('button.edit').click();
      cy.get('@needsBedFileCell').find('button.toggle').click();

      cy.wait('@toggleNeedsBedFile').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
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
      });
      waitForReloadShowing('@tableCells', 5, 'SINGLE; PAIRED; MATE_PAIR');

      // create import alias
      const importAlias = Cypress._.random(0, 1e6);
      cy.intercept('/metaDataFields/createSeqTypeImportAlias/*').as('createImportAlias');

      cy.get('@tableCells').eq(9).as('importAliasCell');
      cy.get('@importAliasCell').find('button.add').click();
      cy.get('@importAliasCell').find('input[name=importAlias]').type(importAlias);
      cy.get('@importAliasCell').find('button.save').click();

      cy.wait('@createImportAlias').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      waitForReloadShowing('@tableCells', 8, importAlias);

      // Change legacy state
      cy.intercept('/metaDataFields/changeSeqTypeLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(10).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
      waitForReloadShowing('@tableCells', 10, 'true');

      // Check all changes
      cy.get('@tableCells').eq(1).contains('false');
      cy.get('@tableCells').eq(2).contains('false');
      cy.get('@tableCells').eq(3).contains('false');
      cy.get('@tableCells').eq(5).contains('SINGLE; PAIRED; MATE_PAIR');
      cy.get('@tableCells').eq(8).contains(importAlias);
      cy.get('@tableCells').eq(10).contains('true');

      // Reset legacy status
      cy.intercept('/metaDataFields/changeSeqTypeLegacyState*').as('changeLegacyState');
      cy.get('@tableCells').eq(10).find('label').click();
      cy.wait('@changeLegacyState').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
      });
    });

    it('should add a new seq type', () => {
      cy.visit('/metaDataFields');
      openTab('Seq. Types', 'seqTypes');

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

      // the creation redirects to the page of the seq types, only that page shows the new seq type
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

    it('should add an identifier to a software tool', () => {
      cy.intercept('POST', '/softwareTool/createSoftwareToolIdentifier*').as('createIdentifier');
      cy.visit('/softwareTool/list');
      const identifier = `identifier-${Cypress._.random(0, 1e6)}`;

      cy.get('.edit-switch-new-free-text-value').first().closest('tr').as('softwareToolRow');
      cy.get('@softwareToolRow').find('.edit-switch-new-free-text-value button.insert').click();
      cy.get('@softwareToolRow').find('.edit-switch-new-free-text-value input[name=value]').type(identifier);
      cy.get('@softwareToolRow').find('.edit-switch-new-free-text-value button.save').click();

      cy.wait('@createIdentifier').its('response.statusCode').should('eq', 200);
      // the page is reloaded after the identifier is stored and lists it for the software tool
      cy.get('@softwareToolRow').find('td').eq(1).should('contain', identifier);
    });

    it('should keep the page and show the error, when an identifier of a software tool cannot be stored', () => {
      cy.intercept('POST', '/softwareTool/createSoftwareToolIdentifier*').as('createIdentifier');
      cy.visit('/softwareTool/list');
      // a reload would remove this marker
      cy.window().then((win) => {
        win.notReloaded = true;
      });

      cy.get('.edit-switch-new-free-text-value').first().closest('tr').as('softwareToolRow');
      cy.get('@softwareToolRow').find('.edit-switch-new-free-text-value button.insert').click();
      // an empty identifier is rejected
      cy.get('@softwareToolRow').find('.edit-switch-new-free-text-value button.save').click();

      // the blank identifier fails the validation of the command
      cy.wait('@createIdentifier').its('response.statusCode').should('eq', 406);
      // the page has no toast box, so the editor shows the error in the info box
      cy.get('#infoBox .errors').should('be.visible').and('contain.text', 'An error occurred while processing the data');
      // a successful save reloads the page as soon as the answer arrived, the wait gives a reload the time to show up
      cy.wait(1000);
      cy.window().its('notReloaded').should('eq', true);
      cy.get('#infoBox .errors').should('be.visible');
    });

    // Check for download files

    it('should visit the software page and download the csv', () => {
      cy.visit('/softwareTool/list');

      cy.get('#metadatafields-datatable').should('exist');
      cy.get('div#metadatafields-datatable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('softwareTools.json');
    });

    it('should visit the library preparation kit page and download the csv', () => {
      cy.visit('/metaDataFields/libraryPreparationKits');

      cy.get('#metadatafields-datatable').should('exist');
      cy.get('div#metadatafields-datatable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('metadataLibraryPreparationKit.json');
    });

    it('should visit the antibody target page and download the csv', () => {
      cy.visit('/metaDataFields/antibodyTargets');

      cy.get('#metadatafields-datatable').should('exist');
      cy.get('div#metadatafields-datatable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('metadataAntibodyTarget.json');
    });

    it('should visit the sequencing center page and download the csv', () => {
      cy.visit('/metaDataFields/seqCenters');

      cy.get('#metadatafields-datatable').should('exist');
      cy.get('div#metadatafields-datatable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('metadataSequencingCenter.json');
    });

    it('should visit the sequencing platforms page and download the csv', () => {
      cy.visit('/metaDataFields/seqPlatforms');

      cy.get('#metadatafields-datatable').should('exist');
      cy.get('div#metadatafields-datatable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('metadataSequencingPlatforms.json');
    });

    it('should visit the sequencing types page and download the csv', () => {
      cy.visit('/metaDataFields/seqTypes');

      cy.get('#metadatafields-datatable').should('exist');
      cy.get('div#metadatafields-datatable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('metadataSequencingTypes.json');
    });

    it('should visit the reference genome page and download the csv', () => {
      cy.visit('/referenceGenome/index');

      cy.get('#referenceGenome-datatable').should('exist');
      cy.get('div#referenceGenome-datatable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('metadataReferenceGenome.json');
    });
  });

  context('when user is a normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
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
      cy.checkAccessDenied('/softwareTool/list');
      cy.checkAccessDenied('/softwareTool/changeSoftwareToolLegacyState');
      cy.checkAccessDenied('/softwareTool/createSoftwareTool');
      cy.checkAccessDenied('/softwareTool/updateSoftwareTool');
      cy.checkAccessDenied('/softwareTool/updateSoftwareToolIdentifier');
      cy.checkAccessDenied('/softwareTool/createSoftwareToolIdentifier');
    });
  });
});

/**
 * Opens a tab of the page. The tabs are links to separate pages, the location shows that the page of the tab is shown.
 */
const openTab = (label, action) => {
  'use strict';

  cy.get('.tab-menu a').contains(label).click();
  cy.location('pathname').should('eq', `/metaDataFields/${action}`);
};

/**
 * Saving a value reloads the page shortly after the request has finished. The page shown before still has the old
 * value, so waiting for the new one makes sure that the next steps are performed on the reloaded page.
 */
const waitForReloadShowing = (tableCellsAlias, cellIndex, value) => {
  'use strict';

  // `contains` normalizes the whitespace, some cells keep the whitespace of the value
  cy.get(tableCellsAlias).eq(cellIndex).contains(value);
};
