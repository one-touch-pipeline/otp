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

describe('Check trigger alignment page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should search seq tracks by project and seqType and trigger alignment', () => {
      cy.visit('/triggerAlignment/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByProjectSeqType*').as('search');
      cy.intercept('/triggerAlignment/generateWarnings*').as('warnings');
      cy.intercept('/triggerAlignment/triggerAlignment*').as('triggerAlignment');

      cy.fixture('triggerAlignment.json').then((alignment) => {
        cy.get('select#project').select(alignment[0].project, { force: true });
        cy.get('select#seqTypeProject').select(alignment[0].seqType, { force: true });
        cy.get('button#searchSeqTrackButton').click();

        cy.wait('@search').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });

        cy.wait('@warnings').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Wait until table is rendered
        cy.get('div#seqTrackTable_processing').should('not.be.visible');
        cy.get('#warnAreaAccordion').should('not.be.visible');

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
        cy.get('input#withdrawBamFiles1').click();

        cy.get('button#triggerAlignmentButton').click();

        cy.wait('@triggerAlignment').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 12)
          .each((row) => {
            cy.wrap(row).contains('skip').contains('since fastqc already exist');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 6);
      });
    });

    it('should search seq tracks by pid and seq. type and trigger alignment', () => {
      cy.visit('/triggerAlignment/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByPidSeqType*').as('search');
      cy.intercept('/triggerAlignment/generateWarnings*').as('warnings');
      cy.intercept('/triggerAlignment/triggerAlignment*').as('triggerAlignment');

      cy.fixture('triggerAlignment.json').then((alignment) => {
        cy.get('a#pid-tab').click();

        cy.get('textarea#pid-selection').clear().type(alignment[1].pid.join(';'));
        cy.get('select#seqTypePid').select(alignment[1].seqType, { force: true });
        cy.get('button#searchSeqTrackButton').click();

        cy.wait('@search').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });

        cy.wait('@warnings').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Wait until table is rendered
        cy.get('div#seqTrackTable_processing').should('not.be.visible');

        cy.get('table#seqTrackTable').find('tbody tr').should('have.length', 12)
          .each((row) => {
            cy.wrap(row).find('td').eq(2).should('satisfy', (el) => alignment[1].pid.includes(el[0].innerText));
            cy.wrap(row).find('td').eq(4).contains(alignment[1].seqType);
          });

        cy.get('table#bamTable').find('tbody tr').should('have.length', 6)
          .each((row) => {
            cy.wrap(row).find('td').eq(2).should('satisfy', (el) => alignment[1].pid.includes(el[0].innerText));
          });

        // TODO otp-2027: Next line should be commented in, when the bug is fixed
        // cy.get('#warnAreaAccordion').should('not.be.visible');

        cy.get('input#ignoreSeqPlatformGroup').uncheck();
        cy.get('input#withdrawBamFiles1').click();

        cy.get('button#triggerAlignmentButton').click();

        cy.wait('@triggerAlignment').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 12)
          .each((row) => {
            cy.wrap(row).contains('skip').contains('since fastqc already exist');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 6);
      });
    });

    it('should search seq tracks by seqTrack ids and trigger alignment', () => {
      cy.visit('/triggerAlignment/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackBySeqTrackId*').as('search');
      cy.intercept('/triggerAlignment/generateWarnings*').as('warnings');
      cy.intercept('/triggerAlignment/triggerAlignment*').as('triggerAlignment');

      cy.fixture('triggerAlignment.json').then((alignment) => {
        cy.get('a#seqtrack-id-tab').click();

        cy.get('textarea#seqTrackId-selection').clear().type(alignment[2].seqTrackIds.join('\t'));
        cy.get('button#searchSeqTrackButton').click();

        cy.wait('@search').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });

        cy.wait('@warnings').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Wait until table is rendered
        cy.get('div#seqTrackTable_processing').should('not.be.visible');

        cy.get('table#seqTrackTable').find('tbody tr').should('have.length', 2)
          .each((row) => {
            cy.wrap(row).find('td').eq(0).should('satisfy', (el) => alignment[2].seqTrackIds.includes(el[0].innerText));
          });
      });

      cy.get('input#ignoreSeqPlatformGroup').uncheck();
      cy.get('input#withdrawBamFiles1').click();

      cy.get('button#triggerAlignmentButton').click();

      cy.wait('@triggerAlignment').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.get('#warnAreaAccordion').should('not.be.visible');
      cy.get('#infos li').should('not.be.empty');
      cy.get('#resultWarning li').should('have.length', 2)
        .each((row) => {
          cy.wrap(row).contains('skip').contains('since fastqc already exist');
        });
      cy.get('#resultWorkPackageList li').should('have.length', 2);
    });

    it('should search seq tracks by ilse number and trigger alignment', () => {
      cy.visit('/triggerAlignment/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByIlseNumber*').as('search');
      cy.intercept('/triggerAlignment/generateWarnings*').as('warnings');
      cy.intercept('/triggerAlignment/triggerAlignment*').as('triggerAlignment');

      cy.fixture('triggerAlignment.json').then((alignment) => {
        cy.get('a#ilse-tab').click();

        cy.get('textarea#ilse-selection').clear().type(alignment[3].ilseNumbers.join('\t'));
        cy.get('button#searchSeqTrackButton').click();

        cy.wait('@search').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });

        cy.wait('@warnings').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Wait until table is rendered
        cy.get('div#seqTrackTable_processing').should('not.be.visible');

        cy.get('div#seqTrackTable_wrapper').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(7).should('satisfy', (el) => alignment[3].ilseNumbers.includes(el[0].innerText));
        });

        cy.get('input#ignoreSeqPlatformGroup').uncheck();
        cy.get('input#withdrawBamFiles1').click();

        cy.get('button#triggerAlignmentButton').click();

        cy.wait('@triggerAlignment').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#warnAreaAccordion').should('not.be.visible');
        cy.get('div.otpWarningToast').should('be.visible').contains('No alignment workflow has been started');
        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 1).should('have.text', 'none');
        cy.get('#resultWorkPackageList li').should('have.length', 1).should('have.text', 'none');
      });
    });

    it('should search seq tracks by multi input and trigger alignment', () => {
      cy.visit('/triggerAlignment/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByMultiInput*').as('search');
      cy.intercept('/triggerAlignment/generateWarnings*').as('warnings');
      cy.intercept('/triggerAlignment/triggerAlignment*').as('triggerAlignment');

      cy.fixture('triggerAlignment.json').then((alignment) => {
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
        cy.get('button#searchSeqTrackButton').click();

        cy.wait('@search').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });

        cy.wait('@warnings').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Wait until table is rendered
        cy.get('div#seqTrackTable_processing').should('not.be.visible');

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
        cy.get('input#withdrawBamFiles1').click();

        cy.get('button#triggerAlignmentButton').click();

        cy.wait('@triggerAlignment').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 2)
          .each((row) => {
            cy.wrap(row).contains('skip').contains('since fastqc already exist');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 1);
      });
    });

    it('should search seq tracks by BAM ID and trigger alignment', () => {
      cy.visit('/triggerAlignment/index');
      cy.intercept('/searchSeqTrack/searchSeqTrackByBamId*').as('search');
      cy.intercept('/triggerAlignment/generateWarnings*').as('warnings');
      cy.intercept('/triggerAlignment/triggerAlignment*').as('triggerAlignment');

      cy.fixture('triggerAlignment.json').then((alignment) => {
        cy.get('a#bam-tab').click();

        cy.get('textarea#bam-selection').clear().type(alignment[5].bamIds.join('\t'));
        cy.get('button#searchSeqTrackButton').click();

        cy.wait('@search').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
        });

        cy.wait('@warnings').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Wait until table is rendered
        cy.get('div#seqTrackTable_processing').should('not.be.visible');

        cy.get('div#seqTrackTable_wrapper').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(14).should('satisfy', (el) => alignment[5].bamIds.includes(el[0].innerText));
        });

        cy.get('div#bamTable_wrapper').find('tbody tr').each((row) => {
          cy.wrap(row).find('td').eq(0).should('satisfy', (el) => alignment[5].bamIds.includes(el[0].innerText));
        });

        cy.get('input#ignoreSeqPlatformGroup').uncheck();
        cy.get('input#withdrawBamFiles1').click();

        cy.get('button#triggerAlignmentButton').click();

        cy.wait('@triggerAlignment').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        cy.get('#warnAreaAccordion').should('not.be.visible');
        cy.get('#infos li').should('not.be.empty');
        cy.get('#resultWarning li').should('have.length', 2)
          .each((row) => {
            cy.wrap(row).contains('skip').contains('since fastqc already exist');
          });
        cy.get('#resultWorkPackageList li').should('have.length', 1);
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/triggerAlignment/index');
      cy.checkAccessDenied('/triggerAlignment/generateWarnings');
      cy.checkAccessDenied('/triggerAlignment/triggerAlignment');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByProjectSeqType');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByPidSeqType');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackBySeqTrackId');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByIlseNumber');
      cy.checkAccessDenied('/searchSeqTrack/searchSeqTrackByMultiInput');
    });
  });
});
