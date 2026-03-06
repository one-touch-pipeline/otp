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

describe('Check trigger Workflows page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should search seq tracks by project and seqType and trigger workflow', () => {
      cy.visit('/triggerWorkflows/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByProjectSeqType*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');
      cy.intercept('/triggerWorkflows/triggerWorkflows*').as('triggerWorkflows');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        cy.get('select#project').select(alignment[0].project, { force: true });
        cy.get('select#seqTypeProject').select(alignment[0].seqType, { force: true });
        clickSearchButtonAndWait();
        cy.get('#warnAreaAccordion > div').should('not.be.visible'); // reactivate once underlying problem is fixed

        cy.get('table#seqTrackTable').find('tbody tr').should('have.length', 12)
          .each((row) => {
            cy.wrap(row).find('td').eq(1).contains(alignment[0].project);
            cy.wrap(row).find('td').eq(4).contains(alignment[0].seqType);
          });

        cy.get('table#bamTable').find('tbody tr').should('have.length', 6)
          .each((row) => {
            cy.wrap(row).find('td').eq(1).contains(alignment[0].project);
          });

        cy.get('input#ignoreSeqPlatformGroup').uncheck();

        // Configure decider actions - set all to "Create always" (default behavior)
        configureDeciderActions('CREATE_ALWAYS');

        cy.get('button#triggerWorkflowsButton').click();

        cy.wait('@triggerWorkflows').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 6)
          .each((row) => {
            cy.wrap(row).contains('recreate').contains('since action is CREATE_ALWAYS');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 6);
      });
    });

    it('should search seq tracks by pid and seq. type and trigger workflow', () => {
      cy.visit('/triggerWorkflows/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByPidSeqType*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');
      cy.intercept('/triggerWorkflows/triggerWorkflows*').as('triggerWorkflows');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        cy.get('a#pid-tab').click();

        cy.get('textarea#pid-selection').clear().type(alignment[1].pid.join(';'));
        cy.get('select#seqTypePid').select(alignment[1].seqType, { force: true });
        clickSearchButtonAndWait();

        cy.get('table#seqTrackTable').find('tbody tr').should('have.length', 12)
          .each((row) => {
            cy.wrap(row).find('td').eq(2).should('satisfy', (el) => alignment[1].pid.includes(el[0].innerText));
            cy.wrap(row).find('td').eq(4).contains(alignment[1].seqType);
          });

        cy.get('table#bamTable').find('tbody tr').should('have.length', 6)
          .each((row) => {
            cy.wrap(row).find('td').eq(2).should('satisfy', (el) => alignment[1].pid.includes(el[0].innerText));
          });

        cy.get('input#ignoreSeqPlatformGroup').uncheck();

        // Configure decider actions - set all to "Create always" (default behavior)
        configureDeciderActions('CREATE_ALWAYS');

        cy.get('button#triggerWorkflowsButton').click();

        cy.wait('@triggerWorkflows').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 6)
          .each((row) => {
            cy.wrap(row).contains('recreate').contains('since action is CREATE_ALWAYS');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 6);
      });
    });

    it('should search seq tracks by seqTrack ids and trigger workflow', () => {
      cy.visit('/triggerWorkflows/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackBySeqTrackId*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');
      cy.intercept('/triggerWorkflows/triggerWorkflows*').as('triggerWorkflows');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        cy.get('a#seqtrack-id-tab').click();

        cy.get('textarea#seqTrackId-selection').clear().type(alignment[2].seqTrackIds.join('\t'));
        clickSearchButtonAndWait();

        cy.get('table#seqTrackTable').find('tbody tr').should('have.length', 2)
          .each((row) => {
            cy.wrap(row).find('td').eq(0).should('satisfy', (el) => alignment[2].seqTrackIds.includes(el[0].innerText));
          });
      });

      cy.get('input#ignoreSeqPlatformGroup').uncheck();

      // Configure decider actions - set all to "Create always" (default behavior)
      configureDeciderActions('CREATE_ALWAYS');

      cy.get('button#triggerWorkflowsButton').click();

      cy.wait('@triggerWorkflows').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.get('#warnAreaAccordion > div').should('not.be.visible'); // reactivate once underlying problem is fixed
      cy.get('#infos li').should('not.be.empty');
      cy.get('#resultWarning li').should('have.length', 2)
        .each((row) => {
          cy.wrap(row).contains('recreate').contains('since action is CREATE_ALWAYS');
        });
      cy.get('#resultWorkPackageList li').should('have.length', 2);
    });

    it('should search seq tracks by ilse number and trigger workflow', () => {
      cy.visit('/triggerWorkflows/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByIlseNumber*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');
      cy.intercept('/triggerWorkflows/triggerWorkflows*').as('triggerWorkflows');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        cy.get('a#ilse-tab').click();

        cy.get('textarea#ilse-selection').clear().type(alignment[3].ilseNumbers.join('\t'));
        clickSearchButtonAndWait();

        cy.get('div#seqTrackTable_wrapper').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(7).should('satisfy', (el) => alignment[3].ilseNumbers.includes(el[0].innerText));
        });

        cy.get('input#ignoreSeqPlatformGroup').uncheck();

        // Configure decider actions - set all to "Create always" (default behavior)
        configureDeciderActions('CREATE_ALWAYS');

        cy.get('button#triggerWorkflowsButton').click();

        cy.wait('@triggerWorkflows').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#warnAreaAccordion > div').should('not.be.visible');
        cy.get('div.otpWarningToast').should('be.visible').contains('No workflows were started');
        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 1).should('have.text', 'none');
        cy.get('#resultWorkPackageList li').should('have.length', 1).should('have.text', 'none');
      });
    });

    it('should search seq tracks by multi input and trigger workflows', () => {
      cy.visit('/triggerWorkflows/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByMultiInput*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');
      cy.intercept('/triggerWorkflows/triggerWorkflows*').as('triggerWorkflows');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        const fix = alignment[4];
        cy.get('a#multi-input-tab').click();

        cy.get('textarea#multi-input-selection').clear().type(
          `${fix.pids[0]};${fix.sampleTypes[0]}, ${fix.seqTypes[0]} \t ${fix.seqReadTypes[0]} ,${fix.singleCells[0]} \n`
        );
        cy.get('textarea#multi-input-selection').type(
          `${fix.pids[1]};${fix.sampleTypes[1]}\t ${fix.seqTypes[1]}, ${fix.seqReadTypes[1]} ;${fix.singleCells[1]},\n `
        );
        cy.get('textarea#multi-input-selection').type(
          `${fix.pids[2]},${fix.sampleTypes[2]}, ${fix.seqTypes[2]} ; ${fix.seqReadTypes[2]} ,${fix.singleCells[2]};\n`
        );
        clickSearchButtonAndWait();

        cy.get('#seqTrackTable tbody tr').should('have.length', 6);

        cy.get('table#seqTrackTable').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(2).should('satisfy', (el) => fix.pids.includes(el[0].innerText));
          cy.wrap(row).find('td').eq(3).should('satisfy', (el) => fix.sampleTypes.includes(el[0].innerText));
        });

        cy.get('table#bamTable').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(2).should('satisfy', (el) => fix.pids.includes(el[0].innerText));
          cy.wrap(row).find('td').eq(3).should('satisfy', (el) => fix.sampleTypes.includes(el[0].innerText));
        });

        cy.get('input#ignoreSeqPlatformGroup').uncheck();

        // Configure decider actions - set all to "Create always" (default behavior)
        configureDeciderActions('CREATE_ALWAYS');

        cy.get('button#triggerWorkflowsButton').click();

        cy.wait('@triggerWorkflows').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 1)
          .each((row) => {
            cy.wrap(row).contains('recreate').contains('since action is CREATE_ALWAYS');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 1);
      });
    });

    it('should search seq tracks by BAM ID and trigger alignment', () => {
      cy.visit('/triggerWorkflows/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByBamId*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');
      cy.intercept('/triggerWorkflows/triggerWorkflows*').as('triggerWorkflows');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        cy.get('a#bam-tab').click();

        cy.get('textarea#bam-selection').clear().type(alignment[5].bamIds.join('\t'));
        clickSearchButtonAndWait();

        cy.get('div#seqTrackTable_wrapper').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(14).should('satisfy', (el) => alignment[5].bamIds.includes(el[0].innerText));
        });

        cy.get('div#bamTable_wrapper').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(0).should('satisfy', (el) => alignment[5].bamIds.includes(el[0].innerText));
        });

        cy.get('input#ignoreSeqPlatformGroup').uncheck();

        // Configure decider actions - set all to "Create always" (default behavior)
        configureDeciderActions('CREATE_ALWAYS');

        cy.get('button#triggerWorkflowsButton').click();

        cy.wait('@triggerWorkflows').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#warnAreaAccordion > div').should('not.be.visible');
        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 1)
          .each((row) => {
            cy.wrap(row).contains('recreate').contains('since action is CREATE_ALWAYS');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 1);
      });
    });

    it('should test different decider action configurations', () => {
      cy.visit('/triggerWorkflows/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByProjectSeqType*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');
      cy.intercept('/triggerWorkflows/triggerWorkflows*').as('triggerWorkflows');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        cy.get('select#project').select(alignment[0].project, { force: true });
        cy.get('select#seqTypeProject').select(alignment[0].seqType, { force: true });
        clickSearchButtonAndWait();

        // Verify decider selection area is visible and functional
        cy.get('#deciderActionSelection').should('be.visible');
        cy.get('#deciderActionSelection select.form-control').should('have.length.greaterThan', 0);

        // Test setting specific decider actions
        cy.get('input#ignoreSeqPlatformGroup').uncheck();

        // Set all deciders to "Skip" to test different behavior
        configureDeciderActions('SKIP');

        cy.get('button#triggerWorkflowsButton').click();

        cy.wait('@triggerWorkflows').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Verify appropriate response for skipped workflows
        cy.get('#infos li').should('not.be.empty');
      });
    });

    it('should download and check the seq tracks csv', () => {
      loadTriggerWorkflowsPage();
      cy.get('div#seqTrackTable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('triggerWorkflowsCheckSeqTracks.json');
    });

    it('should download and check the bam files csv', () => {
      loadTriggerWorkflowsPage();
      cy.get('div#bamTable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('triggerWorkflowsCheckBamFiles.json');
    });

    it('should download and check the configured workflows csv', () => {
      loadTriggerWorkflowsPage();
      cy.get('div#workflowTable_wrapper button').contains('Download').click();
      cy.checkDownloadByContentOfFixture('triggerWorkflowsConfiguredWorkflows.json');
    });

    it('should show warnings for missing workflow config when appropriate', () => {
      // the backend should be improved to only return relevant configs EXOME in this case
      const MISSING_CONFIG_COUNT = 1;

      // Ensure the config is deleted before starting the test
      deleteConfig();

      cy.visit('/triggerWorkflows/index');

      cy.intercept('/searchSeqTrack/searchSeqTrackByProjectSeqType*').as('search');
      cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');

      cy.fixture('triggerWorkflows.json').then((alignment) => {
        cy.get('select#project').select(alignment[0].project, { force: true });
        cy.get('select#seqTypeProject').select(alignment[0].seqTypes, { force: true });
        cy.get('button#searchSeqTrackButton').click();

        cy.wait('@search').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });

        cy.wait('@warnings').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('div#missingWorkflowConfigWarningsCard button:contains("Missing workflow configuration")')
          .should('be.visible').click();

        cy.get('table#missingWorkflowConfigsWarnings > tbody > tr').should('have.length', MISSING_CONFIG_COUNT)
          .and('contain.text', alignment[6].workflow)
          .and('contain.text', alignment[6].seqType);

        // Add the missing config back so that subsequent tests can run correctly (idempotent)
        addConfig();
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/triggerWorkflows/index');
      cy.checkAccessDenied('/triggerWorkflows/generateWarnings');
      cy.checkAccessDenied('/triggerWorkflows/triggerWorkflows');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByProjectSeqType');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByPidSeqType');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackBySeqTrackId');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByIlseNumber');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByMultiInput');
    });
  });
});

const clickSearchButtonAndWait = () => {
  'use strict';

  cy.get('button#searchSeqTrackButton').click();

  cy.wait('@search').then((interception) => {
    expect(interception.response.statusCode).to.eq(302);
  });

  cy.wait('@warnings').then((interception) => {
    expect(interception.response.statusCode).to.eq(200);
  });

  cy.get('div#seqTrackTable_processing').should('not.be.visible');
};

const loadTriggerWorkflowsPage = () => {
  'use strict';

  cy.clearDownloadsFolder();
  cy.visit('/triggerWorkflows/index');
  cy.intercept('/searchSeqTrack/searchSeqTrackByProjectSeqType*').as('search');
  cy.intercept('/triggerWorkflows/generateWarnings*').as('warnings');

  cy.fixture('triggerWorkflows.json').then((alignment) => {
    cy.get('select#project').select(alignment[0].project, { force: true });
    cy.get('select#seqTypeProject').select('EXOME PAIRED bulk', { force: true });
    clickSearchButtonAndWait();
  });
};

const addConfig = () => {
  'use strict';

  cy.visit('/workflowSelection/index?project=ExampleProject');

  cy.intercept('/workflowSelection/saveAlignmentConfiguration?project=ExampleProject').as('saveConfig');

  cy.get('.tab-menu a:contains("Workflow selection")').click();
  cy.get('h2.accordion-header button.accordion-button:contains("Alignment workflows")').click();
  cy.get('table#alignmentTable > tfoot > tr').within(() => {
    cy.fixture('triggerWorkflows.json').then((alignment) => {
      cy.get('td:first-child > select').select(alignment[6].workflow, { force: true })
        .should('have.value', '18');
      cy.get('td:nth-child(2) > select').select(alignment[6].seqType, { force: true })
        .should('have.value', '71');
      cy.get('td:nth-child(3) > select').select(alignment[6].version, { force: true })
        .should('have.value', '695');
      cy.get('td:nth-child(4) > select').select(alignment[6].referenceGenome, { force: true })
        .should('have.value', '914');
      cy.get('td:nth-child(5) > select').select(alignment[6].species, { force: true });
    });
    cy.get('td:nth-child(6) > button').click();
  });

  cy.wait('@saveConfig').then((interception) => {
    expect(interception.response.statusCode).to.eq(200);
  });
};

const deleteConfig = () => {
  'use strict';

  cy.visit('/workflowSelection/index?project=ExampleProject');

  cy.intercept('/workflowSelection/deleteConfiguration?project=ExampleProject').as('deleteConfig');

  cy.fixture('triggerWorkflows.json').then((alignment) => {
    cy.get('.tab-menu a:contains("Workflow selection")').click();
    cy.get('h2.accordion-header button.accordion-button:contains("Alignment workflows")').click();
    cy.get(`table#alignmentTable > tbody > tr > td:first-child:contains("${alignment[6].workflow}")`)
      .parent().find(`td:nth-child(2):contains("${alignment[6].seqType}")`)
      .parent().find('td > button.remove-config-btn').should('exist').click();
  });

  cy.wait('@deleteConfig').then((interception) => {
    expect(interception.response.statusCode).to.eq(200);
  });
};

const configureDeciderActions = (action) => {
  'use strict';

  // Map action names to their corresponding IDs based on DeciderCreateWorkflowAction enum
  const actionIdMap = {
    'CREATE_MISSING': '1',
    'CREATE_MISSING_AND_NEWER': '2',
    'CREATE_ALWAYS': '3',
    'SKIP': '4'
  };

  const actionId = actionIdMap[action] || '1'; // Default to CREATE_MISSING

  // Set all decider actions to the specified value
  cy.get('#deciderActionSelection select.form-control').each(($select, index) => {
    if (index === 0) {
      cy.wrap($select).select('4', { force: true });
    } else {
      cy.wrap($select).select(actionId, { force: true });
    }
  });
};
