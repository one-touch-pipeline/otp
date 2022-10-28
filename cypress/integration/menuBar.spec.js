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

describe('Click all menu items in the menu bar', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/');
    });

    it('should look at the number of menu items', () => {
      const menuBar = 'div.menuContainer';
      const menuBarItem = 'li:not(.nav_container)';
      cy.get(menuBar).find(menuBarItem)
        .should('have.length', 71).and('contain', 'a');
    });

    it('should click the Overview menu item', () => {
      cy.get('li.menuContainerItem').contains('Overview').click();
      cy.checkPage('/sampleOverview/index');
    });

    it('should click the Individuals menu item', () => {
      cy.get('li.menuContainerItem').contains('Individuals').click();
      cy.checkPage('/individual/list');
    });

    it('should click the Sequences menu item', () => {
      cy.get('li.menuContainerItem').contains('Sequences').click();
      cy.checkPage('/sequence/index');
    });

    it('should check the number of menu items and click all Project menu items', () => {
      cy.get('li.navigation').contains('Project').parent().find('li')
        .should('have.length', 10);
      cy.get('.navigation li').contains('Project Specific Statistics').click({ force: true });
      cy.checkPage('/projectOverview/index');

      cy.get('.navigation li').contains('Project Config').click({ force: true });
      cy.checkPage('/projectConfig/index');

      cy.get('.navigation li').contains('Workflow Config').click({ force: true });
      cy.checkPage('/alignmentConfigurationOverview/index');

      cy.get('.navigation li').contains('User Management').click({ force: true });
      cy.checkPage('/projectUser/index');

      cy.get('.navigation li').contains('Project request').click({ force: true });
      cy.checkPage('/projectRequest/index');

      cy.get('.navigation li').contains('Sample Name').click({ force: true });
      cy.checkPage('/sampleIdentifierOverview/index');

      cy.get('.navigation li').contains('Project Info').click({ force: true });
      cy.checkPage('/projectInfo/list');

      cy.get('.navigation li').contains('Data Transfer').click({ force: true });
      cy.checkPage('/dataTransfer/index');

      cy.get('.navigation li').contains('Configure Project Information').click({ force: true });
      cy.checkPage('/projectFields/index');
    });

    it('should check the number of menu items and click all Results menu items', () => {
      cy.get('li.navigation').contains('Results').parent().find('li')
        .should('have.length', 7);
      cy.get('.navigation li').contains('Alignment Quality Control').click({ force: true });
      cy.checkPage('/alignmentQualityOverview/index');

      cy.get('.navigation li').contains('Cell Ranger Final Run Selection').click({ force: true });
      cy.checkPage('/cellRanger/finalRunSelection');

      cy.get('.navigation li').contains('SNV Results').click({ force: true });
      cy.checkPage('/snv/results');

      cy.get('.navigation li').contains('Indel Results').click({ force: true });
      cy.checkPage('/indel/results');

      cy.get('.navigation li').contains('CNV Results (from ACEseq)').click({ force: true });
      cy.checkPage('/aceseq/results');

      cy.get('.navigation li').contains('SV Results (from SOPHIA)').click({ force: true });
      cy.checkPage('/sophia/results');

      cy.get('.navigation li').contains('Mutational Signatures Results (from runYapsa').click({ force: true });
      cy.checkPage('/runYapsa/results');
    });

    it('should click the EGA menu item', () => {
      cy.get('li.menuContainerItem').contains('EGA').click();
      cy.checkPage('/egaSubmission/overview');
    });

    it('should check the number of menu items and click all Statistics menu items', () => {
      cy.get('li.navigation').contains('Statistics ▽').parent().find('li')
        .should('have.length', 4);
      cy.get('.navigation li').contains('Job Statistics').click({ force: true });
      cy.checkPage('/clusterJobGeneral/index');

      cy.get('.navigation li').contains('Job Statistics (jobtype specific)').click({ force: true });
      cy.checkPage('/clusterJobJobTypeSpecific/index');

      cy.get('.navigation li').contains('Processing Time Statistics').click({ force: true });
      cy.checkPage('/processingTimeStatistics/index');

      cy.get('.navigation li').contains('KPI').click({ force: true });
      cy.checkPage('/statistics/kpi');
    });

    it('should check the number of menu items and click all Operator menu items', () => {
      cy.get('li.navigation').contains('Operator').parent().find('li')
        .should('have.length', 13);
      cy.get('.navigation li').contains('FASTQ Import').click({ force: true });
      cy.checkPage('/metadataImport/index');
      cy.go('back');

      cy.get('.navigation li').contains('BAM Import').click({ force: true });
      cy.checkPage('/bamMetadataImport/index');
      cy.go('back');

      cy.get('.navigation li').contains('Blacklisted ILSe No').click({ force: true });
      cy.checkPage('/metadataImport/blacklistedIlseNumbers');

      cy.get('.navigation li').contains('New Project').click({ force: true });
      cy.checkPage('/projectCreation/index');

      cy.get('.navigation li').contains('New Individual').click({ force: true });
      cy.checkPage('/individual/insert');

      cy.get('.navigation li').contains('New Sample').click({ force: true });
      cy.checkPage('/bulkSampleCreation/index');

      cy.get('.navigation li').contains('Workflows').click({ force: true });
      cy.checkPage('/processes/list');

      cy.get('.navigation li').contains('Merge Seq. Platform').click({ force: true });
      cy.checkPage('/defaultSeqPlatformGroup/index');

      cy.get('.navigation li').contains('Metadata').click({ force: true });
      cy.checkPage('/metaDataFields/libraryPreparationKits');

      cy.get('.navigation li').contains('QC Thresholds').click({ force: true });
      cy.checkPage('/qcThreshold/defaultConfiguration');

      cy.get('.navigation li').contains('Project Info Documents').click({ force: true });
      cy.checkPage('/document/manage');
    });

    it('should check the number of menu items and click all Workflow System menu items', () => {
      cy.get('li.navigation').contains('Workflow System').parent().find('li')
        .should('have.length', 11);
      cy.get('.navigation li').contains('Status').click({ force: true });
      cy.checkPage('/systemStatus/index');

      cy.get('.navigation li').contains('Configuration').click({ force: true });
      cy.checkPage('/workflowSystemConfig/index');

      cy.get('.navigation li').contains('Run Overview').click({ force: true });
      cy.checkPage('/workflowRunOverview/index');

      cy.get('.navigation li').contains('Run List').click({ force: true });
      cy.checkPage('/workflowRunList/index');

      cy.get('.navigation li').contains('Pipeline Config').click({ force: true });
      cy.checkPage('/workflowConfig/index');

      cy.get('.navigation li').contains('Pipeline Config Viewer').click({ force: true });
      cy.checkPage('/workflowConfigViewer/index');

      cy.get('.navigation li').contains('Crash Repair').click({ force: true });
      cy.checkPage('/crashRepair/index');

      cy.get('.navigation li').contains('Restart Handler').click({ force: true });
      cy.checkPage('/workflowJobErrorDefinition/index');

      cy.get('.navigation li').contains('Processing Priority').click({ force: true });
      cy.checkPage('/processingPriority/index');
    });

    it('should check the number of menu items and click all Admin menu items', () => {
      cy.get('li.navigation').contains('Admin').parent().find('li')
        .should('have.length', 7);
      cy.get('.navigation li').contains('User Administration').click({ force: true });
      cy.checkPage('/userAdministration/index');

      cy.get('.navigation li').contains('Roles and Groups').click({ force: true });
      cy.checkPage('/roles/index');

      cy.get('.navigation li').contains('Crash Recovery').click({ force: true });
      cy.checkPage('/crashRecovery/index');

      cy.get('.navigation li').contains('Processing Options').click({ force: true });
      cy.checkPage('/processingOption/index');

      cy.get('.navigation li').contains('Job Error Definition').click({ force: true });
      cy.checkPage('/jobErrorDefinition/index');

      cy.get('.navigation li').contains('DICOM Logging').click({ force: true });
      cy.checkPage('/dicom/dictionary');

      cy.get('.navigation li').contains('Plan Server Shutdown').click({ force: true });
      cy.checkPage('/shutdown/index');
    });

    it('should click the Home menu item', () => {
      // Some problem with the session doesn't display Home Button
      cy.get('li.menuContainerItem').contains('Overview').click();
      cy.get('li.menuContainerItem').contains('Home').click();
      cy.checkPage();
    });

    it('should check the number of menu items and click all Info menu items', () => {
      cy.get('li.navigation').contains('Info ▽').parent().find('li')
        .should('have.length', 6);
      cy.get('.navigation li').contains('About').click({ force: true });
      cy.checkPage();

      cy.get('.navigation li').contains('In numbers').click({ force: true });
      cy.checkPage('/info/numbers');

      cy.get('.navigation li').contains('Contact').click({ force: true });
      cy.checkPage('/info/contact');

      cy.get('.navigation li').contains('Imprint').click({ force: true });
      cy.checkPage('/info/imprint');

      cy.get('.navigation li').contains('Partners').click({ force: true });
      cy.checkPage('/info/partners');

      cy.get('.navigation li').contains('Templates').click({ force: true });
      cy.checkPage('/info/templates');
    });

    it('should click the logout menu item', () => {
      // Some problem with the session doesn't display Logout Button
      cy.get('li.menuContainerItem').contains('Overview').click();
      cy.get('li.menuContainerItem').contains('Logout').click();
      cy.checkPage();
    });
  });
});
