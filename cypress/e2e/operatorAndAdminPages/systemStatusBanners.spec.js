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

/**
 * E2E coverage for the global "system status" banners that warn about disabled subsystems.
 *
 * The banners live in the shared `common` layout and are only rendered for administrative users
 * (ROLE_ADMIN or ROLE_OPERATOR, i.e. `SecurityService.hasCurrentUserAdministrativeRoles()`).
 * The re-enable links target ROLE_ADMIN-only pages, so the link itself is rendered only for ROLE_ADMIN;
 * a plain operator sees the banner text without a (dead) link.
 */
describe('Global system status banners', () => {
  'use strict';

  const autoImportOption = 'TICKET_SYSTEM_AUTO_IMPORT_ENABLED';

  const banner = '.otp-system-banner';
  const newsBanner = '.news-banner';
  const autoImportText = 'The auto-import is currently disabled.';
  const workflow_text = 'The workflow system is currently disabled.';
  const newsBannerText = 'Scheduled maintenance tonight.';

  const disableAutoImport = () => {
    cy.setProcessingOption(autoImportOption, 'false');
  };

  const disableWorkflowSystem = () => {
    cy.visit('/systemStatus/index');
    cy.get('body').then(($body) => {
      if ($body.find('input[type="submit"][value="Stop"]').length > 0) {
        cy.get('input[type="submit"][value="Stop"]').first().click();
      }
    });
    // After the (potential) stop, the page must offer to start the system again -> system is disabled.
    cy.get('input[type="submit"][value="Start"]').should('exist');
  };

  before(() => {
    cy.loginAs('operator');
    disableAutoImport();
    disableWorkflowSystem();
  });

  /* TC1: an administrative user sees BOTH banners at the top of the page when the auto-import and
          the workflow system are disabled, each linking to the page where it can be re-enabled. */
  context('TC1 - administrative user with both subsystems disabled', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('shows both status banners at the top of the page', () => {
      cy.visit('/home/index');

      cy.get(banner).should('have.length', 2);
      cy.contains(banner, autoImportText).should('be.visible');
      cy.contains(banner, workflow_text).should('be.visible');
    });

    it('links each banner to the page where the subsystem can be re-enabled', () => {
      cy.visit('/home/index');

      cy.contains(banner, autoImportText).find('a')
        .should('have.attr', 'href').and('include', '/processingOption');
      cy.contains(banner, workflow_text).find('a')
        .should('have.attr', 'href').and('include', '/systemStatus');
    });

    it('renders the banners on both non-bootstrap and bootstrap pages', () => {
      cy.visit('/systemStatus/index'); // `main` (non-bootstrap) layout
      cy.get(banner).should('have.length', 2);

      cy.visit('/workflowRunOverview/index'); // `application` (bootstrap) layout
      cy.get(banner).should('have.length', 2);
    });

    it('removes a banner once its subsystem is re-enabled from the banner link', () => {
      cy.visit('/home/index');
      cy.contains(banner, autoImportText).should('be.visible');

      cy.contains(banner, autoImportText).find('a').click();
      cy.url().should('include', '/processingOption');
      cy.contains('Processing Options').should('exist');

      cy.setProcessingOption(autoImportOption, 'true');
      cy.visit('/home/index');
      cy.contains(autoImportText).should('not.exist');
      cy.contains(banner, workflow_text).should('be.visible');

      cy.setProcessingOption(autoImportOption, 'false');
    });

    it('dismisses a banner via its close button and keeps it hidden while navigating', () => {
      cy.visit('/home/index');
      cy.get(banner).should('have.length', 2);

      cy.contains(banner, autoImportText).find('.otp-system-banner-close').click();
      cy.contains(autoImportText).should('not.exist');
      cy.contains(banner, workflow_text).should('be.visible');

      cy.visit('/systemStatus/index');
      cy.contains(autoImportText).should('not.exist');
      cy.contains(banner, workflow_text).should('be.visible');

      cy.clearAllSessionStorage();
    });
  });

  // TC2: a non-administrative user never sees the banners, even while both subsystems are disabled.
  context('TC2 - non-administrative user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('never shows the status banners even though both subsystems are disabled', () => {
      cy.visit('/home/index');

      cy.get(banner).should('not.exist');
      cy.contains(autoImportText).should('not.exist');
      cy.contains(workflow_text).should('not.exist');
    });
  });

  /* TC3: the warnings float on the black header where the news banner normally sits and, when present, replace
          the news banner for administrative users; other users still get the news banner as usual. */
  context('TC3 - warnings replace the news banner', () => {
    beforeEach(() => {
      cy.intercept('GET', '**/info/newsBanner*', newsBannerText).as('newsBanner');
    });

    it('hides the news banner and shows the warnings instead for an admin', () => {
      cy.loginAs('operator');
      cy.visit('/home/index');
      cy.wait('@newsBanner');

      cy.get(banner).should('have.length', 2);
      cy.get(newsBanner).should('not.be.visible');
    });

    it('still shows the news banner for a non-administrative user without warnings', () => {
      cy.loginAs('user');
      cy.visit('/home/index');
      cy.wait('@newsBanner');

      cy.get(banner).should('not.exist');
      cy.contains(newsBanner, newsBannerText).should('be.visible');
    });
  });
});
