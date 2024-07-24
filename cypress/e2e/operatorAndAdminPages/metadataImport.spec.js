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

describe('Check metadata import page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should create sample identifier on bulk sample creation page', () => {
      cy.intercept('/bulkSampleCreation/index*').as('index');

      cy.visit('/bulkSampleCreation/index');

      cy.get('select#delimiter').select(',', { force: true });
      cy.fixture('bulkSampleCreation.txt').then((text) => {
        cy.get('textarea#sampleText').clear().type(text);
      });

      cy.get('input#createMissingSampleTypes').click();

      cy.get('input#Submit').click();

      cy.get('div#infoBox').contains('Creation succeeded').should('exist');
    });

    it('should successfully edit ticket information on details page', () => {
      cy.visit('/metadataImport/details/925');
      cy.intercept('/metadataImport/updateSeqCenterComment*').as('saveComment');
      cy.intercept('/metadataImport/updateFinalNotificationFlag*').as('updateFinalNotify');
      cy.intercept('/metadataImport/updateAutomaticNotificationFlag*').as('updateNotify');
      cy.intercept('/metadataImport/assignTicketToFastqImportInstance/*').as('assignTicketNumber');
      const successMessageQuery = '#otpToastBox .otpSuccessToast';

      cy.fixture('metadataImport.json').then((fixture) => {
        const { ticket } = fixture;
        cy.get('table#ticket-table tbody tr').as('ticketRows', { static: true });

        cy.get('@ticketRows').filter(':contains("Number")').as('numberRow');
        cy.get('@numberRow').find('button.js-edit').click();
        cy.get('@numberRow').find('input[name="value"]').clear().type(ticket.number);
        cy.get('@numberRow').find('button.save').click();
        cy.wait('@assignTicketNumber').its('response.statusCode').should('eq', 200);
        cy.get(successMessageQuery).should('exist').and('contain.text', 'Success');
        cy.get('@numberRow').contains(ticket.number);

        cy.get('@ticketRows').filter(':contains("Notify automatically")').as('notifyRow');
        cy.get('@notifyRow').find('button.edit').click();
        cy.get('@notifyRow').find('select').select(ticket.notify, { force: true });
        cy.get('@notifyRow').find('button.save').click();
        cy.wait('@updateNotify').its('response.statusCode').should('eq', 200);
        cy.get(successMessageQuery).should('exist').and('contain.text', 'Success');
        cy.get('@notifyRow').contains(ticket.notify);

        cy.get('@ticketRows').filter(':contains("Final notification")').as('finalNotifyRow');
        cy.get('@finalNotifyRow').find('button.edit').click();
        cy.get('@finalNotifyRow').find('select').select(ticket.finalNotify, { force: true });
        cy.get('@finalNotifyRow').find('button.save').click();
        cy.wait('@updateFinalNotify').its('response.statusCode').should('eq', 200);
        cy.get(successMessageQuery).should('exist').and('contain.text', 'Success');
        cy.get('@finalNotifyRow').contains(ticket.finalNotify);

        cy.get('@ticketRows').filter(':contains("comment")').as('commentRow');
        cy.get('@commentRow').find('button.js-edit').click();
        cy.get('@commentRow').find('textarea.edit-switch-input').type(ticket.comment);
        cy.get('@commentRow').find('button.save').click();
        cy.wait('@saveComment').its('response.statusCode').should('eq', 200);
        cy.get(successMessageQuery).should('exist').and('contain.text', 'Success');
        cy.get('@commentRow').contains(ticket.comment);
      });
    });

    it('should fail edit ticket information with invalid data on details page', () => {
      cy.visit('/metadataImport/details/925');
      cy.intercept('/metadataImport/assignTicketToFastqImportInstance/*').as('assignTicketNumber');
      const errorMessageQuery = '#otpToastBox .otpErrorToast';

      cy.fixture('metadataImport.json').then((fixture) => {
        const ticket = fixture.invalidTicket;

        cy.get('table#ticket-table tbody tr').as('ticketRows', { static: true });

        cy.get('@ticketRows').filter(':contains("Number")').as('numberRow');
        cy.get('@numberRow').find('button.js-edit').click();
        cy.get('@numberRow').find('input[name="value"]').clear().type(ticket.number);
        cy.get('@numberRow').find('button.save').click();
        cy.wait('@assignTicketNumber').its('response.statusCode').should('eq', 200);
        cy.get(errorMessageQuery).should('exist').and('contain.text', 'not be stored');
        cy.get('@numberRow').should('not.contain.text', ticket.number);
      });
    });

    it('should successfully prepare and send notification report', () => {
      cy.visit('/metadataImport/details/925');
      cy.intercept('/notification/notificationPreview*').as('notifyPreview');
      cy.intercept('/notification/index*').as('sendNotification');

      cy.get('#notification-selection-container [value="ALIGNMENT"]').uncheck();
      cy.get('#notification-selection-container [value="SNV"]').uncheck();
      cy.get('#notification-selection-container [value="RUN_YAPSA"]').uncheck();
      cy.get('#notification-selection-container [value="SOPHIA"]').uncheck();

      cy.get('input#notificationPreview').click();
      cy.wait('@notifyPreview').its('response.statusCode').should('eq', 200);
      cy.url().should('contain', '/notification/notificationPreview');

      // Send notification
      cy.get('input#send-button').click();
      cy.wait('@sendNotification').its('response.statusCode').should('eq', 302);

      cy.get('#otpToastBox .otpSuccessToast').should('be.visible').and('contain.text', 'Notifications sent');
      cy.url().should('contain', '/metadataImport/details');
    });

    it('should not be able to send invalid notification report', () => {
      cy.visit('/metadataImport/details/925');
      cy.intercept('/notification/notificationPreview*').as('notifyPreview');
      cy.intercept('/notification/index*').as('sendNotification');

      cy.get('#notification-selection-container [value="INSTALLATION"]').uncheck();
      cy.get('#notification-selection-container [value="ALIGNMENT"]').uncheck();
      cy.get('#notification-selection-container [value="SNV"]').uncheck();
      cy.get('#notification-selection-container [value="INDEL"]').uncheck();
      cy.get('#notification-selection-container [value="SOPHIA"]').uncheck();
      cy.get('#notification-selection-container [value="ACESEQ"]').uncheck();
      cy.get('#notification-selection-container [value="SOPHIA"]').uncheck();
      cy.get('#notification-selection-container [name="notifyQcThresholds"]').uncheck();

      cy.get('input#notificationPreview').click();
      cy.wait('@notifyPreview').its('response.statusCode').should('eq', 200);
      cy.url().should('contain', '/notification/notificationPreview');

      // Send notification
      cy.get('input#send-button').should('be.disabled').invoke('removeAttr', 'disabled')
        .click({ force: true });
      cy.wait('@sendNotification').its('response.statusCode').should('eq', 302);

      cy.get('#otpToastBox .otpErrorToast ').should('be.visible').and('contain.text', 'Error when sending');
      cy.url().should('contain', '/metadataImport/details');
    });

    it('should not import data with path, when md5 sum is changed between validation and import', () => {
      cy.visit('/metadataImport/index');

      cy.intercept('/metadataImport/validatePathsOrFiles*').as('validate');
      cy.intercept('/metadataImport/importByPathOrContent*').as('import');

      const ticketNumber = '251';
      const path1 = '/home/otp/filesystem/otp_example_data/example_import_project01_1.csv';
      const path2 = '/home/otp/filesystem/otp_example_data/example_import_project01_2.csv';
      const seqCenterComment = 'Test comment';

      cy.get('#ticketNumber').clear().type(ticketNumber);
      cy.get('#seqCenterComment').clear().type(seqCenterComment);
      cy.get('[name=paths]').eq(0).clear().type(path1);
      cy.get('#path-container button.add-field').click();
      cy.get('[name=paths]').eq(1).clear().type(path2);
      cy.get('input[type=radio][name=directoryStructure]').check('ABSOLUTE_PATH');
      cy.get('button#validate-btn').click();

      cy.wait('@validate').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.wait('@validate').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.get('.path-spinner').should('not.be.visible');
      cy.get('#validate-spinner').should('not.be.visible');
      // change the saved md5 sum, which has the same effect as changing the file on the filesystem
      cy.get('input[name="md5"]').eq(1).clear({ force: true }).type('1234abc', { force: true });
      cy.get('#ignore-warnings-input').check();
      cy.get('button#import-btn').click();

      cy.wait('@import').then((interception) => {
        expect(interception.response.statusCode).to.eq(400);
        cy.location('pathname').should('match', /\/\/?metadataImport\/index/);
      });

      cy.get('#otpToastBox').should('exist');
      cy.get('#validate-btn').should('not.be.disabled');
      cy.get('#import-btn').should('be.disabled');
    });

    it('should import data with path, when required values are set', () => {
      cy.visit('/metadataImport/index');

      cy.intercept('/metadataImport/validatePathsOrFiles*').as('validate');
      cy.intercept('/metadataImport/importByPathOrContent*').as('import');
      cy.intercept('/metadataImport/details/*').as('details');

      cy.get('#ticketNumber').clear().type('123');
      cy.get('[name=paths]').eq(0).clear().type('/home/otp/filesystem/otp_example_data/example_import_project02_1.csv');
      cy.get('input[type=radio][name=directoryStructure]').check('ABSOLUTE_PATH');
      cy.get('button#validate-btn').click();
      cy.get('.path-spinner').should('be.visible');
      cy.get('#validate-spinner').should('be.visible');

      cy.wait('@validate').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
      cy.get('#validate-spinner').should('not.be.visible');
      cy.get('.path-spinner').should('not.be.visible');
      cy.get('#ignore-warnings-input').check();
      cy.get('button#import-btn').click();

      cy.wait('@import').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.wait('@details').then(() => {
        cy.location('pathname').should('match', /\/\/?metadataImport\/details\/\d+/);
      });
    });

    it('should import data with file, when required values are set', () => {
      cy.visit('/metadataImport/index');

      cy.intercept('/metadataImport/validatePathsOrFiles*').as('validate');
      cy.intercept('/metadataImport/importByPathOrContent*').as('import');
      cy.intercept('/metadataImport/multiDetails*').as('multiDetails');

      cy.fixture('file-uploads/example_import_project01_1.csv').as('file1');
      cy.fixture('file-uploads/example_import_project01_2.csv').as('file2');

      cy.get('#ticketNumber').clear().type('245');
      cy.get('input[type=radio][name=metadataFileSource]').check('FILE');
      cy.get('#file-container input[name=contentList]').should('be.visible').eq(0)
        .selectFile('@file1', { action: 'drag-drop' });
      cy.get('#file-container button.add-field').click();
      cy.get('#file-container [name=contentList]').should('be.visible').eq(1)
        .selectFile('@file2', { action: 'drag-drop' });
      cy.get('button#validate-btn').click();
      cy.get('#validate-spinner').should('be.visible');
      cy.get('.content-spinner').should('be.visible');

      cy.wait('@validate').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.get('#validate-spinner').should('not.be.visible');
      cy.get('.content-spinner').should('not.be.visible');
      cy.get('#ignore-warnings-input').check();
      cy.get('button#import-btn').click();

      cy.wait('@import').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.wait('@multiDetails').then(() => {
        cy.location('pathname').should('match', /\/\/?metadataImport\/multiDetails/);
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/metadataImport/index');
      cy.checkAccessDenied('/bulkSampleCreation/index');
      cy.checkAccessDenied('/bulkSampleCreation/submit');
      cy.checkAccessDenied('/metadataImport/validatePathsOrFiles');
      cy.checkAccessDenied('/metadataImport/importByPathOrContent');
      cy.checkAccessDenied('/metadataImport/details');
      cy.checkAccessDenied('/metadataImport/multiDetails');
      cy.checkAccessDenied('/metadataImport/updateSeqCenterComment');
    });
  });
});
