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

describe('Check dataTransfer page', () => {
  'use strict';

  const fixturesFolder = Cypress.config('fixturesFolder');
  const peerInstitution = `E2E Test Institution ${Date.now()}`;
  const toastSuccess = '#otpToastBox .otpSuccessToast';
  const toastError = '#otpToastBox .otpErrorToast';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/dataTransfer/index');
    });

    it('should create a data transfer agreement with a file', () => {
      cy.intercept('POST', '/dataTransfer/addDataTransferAgreement*').as('addDta');

      cy.get('#dtaFileInput').selectFile(`${fixturesFolder}/file-uploads/hello-world.txt`, { force: true });
      cy.get('#partnerInstitutionInput').clear().type(peerInstitution);
      cy.get('form[action*="addDataTransferAgreement"] button[type=submit]').click();

      cy.wait('@addDta').its('response.statusCode').should('eq', 302);
      cy.get('#dtaAccordion').should('contain.text', peerInstitution);
    });

    it('should create two data transfers within the agreement', () => {
      cy.intercept('POST', '/dataTransfer/addTransfer*').as('addTransfer');

      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();
      cy.get('@dtaCard').find('.alignment-new-transfer-btn').click();

      cy.get('@dtaCard').find('#requestedByInput').type('Test Requester');
      cy.get('@dtaCard').find('#ticketIdInput').type('TICKET-E2E-001');
      cy.get('@dtaCard').find('#peerPersonInput').type('Test Person');
      cy.get('@dtaCard').find('form[action*="addTransfer"] input[type=file][name=files]')
        .selectFile(`${fixturesFolder}/file-uploads/hello-world.txt`, { force: true });
      cy.get('@dtaCard').find('form[action*="addTransfer"] button[type=submit]').click();

      cy.wait('@addTransfer').its('response.statusCode').should('eq', 302);

      cy.intercept('POST', '/dataTransfer/addTransfer*').as('addTransfer2');

      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();
      cy.get('@dtaCard').find('.alignment-new-transfer-btn').click();

      cy.get('@dtaCard').find('#requestedByInput').type('Test Requester 2');
      cy.get('@dtaCard').find('#ticketIdInput').type('TICKET-E2E-002');
      cy.get('@dtaCard').find('#peerPersonInput').type('Test Person 2');
      cy.get('@dtaCard').find('form[action*="addTransfer"] input[type=file][name=files]')
        .selectFile(`${fixturesFolder}/file-uploads/hello-world.txt`, { force: true });
      cy.get('@dtaCard').find('form[action*="addTransfer"] button[type=submit]').click();

      cy.wait('@addTransfer2').its('response.statusCode').should('eq', 302);

      cy.contains('#dtaAccordion .card', peerInstitution).find('.btn-link').click();
      cy.contains('#dtaAccordion .card', peerInstitution)
        .find('.transfer-listing > li').should('have.length.at.least', 2);
    });

    it('should add files to an existing data transfer and show success toast', () => {
      cy.intercept('POST', '/dataTransfer/addFilesToTransfer*').as('addFiles');

      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();
      cy.get('@dtaCard').find('.transfer-listing > li').first().as('transfer');

      cy.get('@transfer').find('.alignment-add-files-btn').click();
      cy.get('@transfer').find('input[type=file][name=files]')
        .selectFile(`${fixturesFolder}/file-uploads/hello-world.txt`, { force: true });
      cy.get('@transfer').find('button[onclick*="addFilesToTransfer"]').click();

      cy.wait('@addFiles').its('response.statusCode').should('eq', 200);
      cy.get(toastSuccess).should('be.visible').and('contain.text', 'Files are added');
      cy.get('@transfer').find('[id^="transferDocuments-"] a').should('have.length.at.least', 1);
    });

    it('should show error toast when uploading to a transfer without selecting a file', () => {
      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();
      cy.get('@dtaCard').find('.transfer-listing > li').first().as('transfer');

      cy.get('@transfer').find('.alignment-add-files-btn').click();
      cy.get('@transfer').find('button[onclick*="addFilesToTransfer"]').click();

      cy.get(toastError).should('be.visible').and('contain.text', 'Upload Failed');
    });

    it('should be able to download a transfer file', () => {

      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();

      cy.watchDownloadRequest('/dataTransfer/downloadDataTransferDocument*');
      cy.get('@dtaCard').find('.transfer-listing > li').first()
        .find('[id^="transferDocuments-"] a').first()
        .click();

      cy.checkDownloadByContentOfFixture('dataTransferDocument.json', '', true);

    });

    it('should add a comment to a data transfer and show success toast', () => {
      cy.intercept('POST', '/dataTransfer/updateDataTransferComment*').as('updateComment');
      const comment = `E2E test comment ${Date.now()}`;

      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();
      cy.get('@dtaCard').find('.transfer-listing > li').first().as('transfer');

      cy.get('@transfer').find('.edit-switch-text-area .edit-switch-label button.js-edit').click();
      cy.get('@transfer').find('.edit-switch-text-area .edit-switch-editor textarea').clear().type(comment);
      cy.get('@transfer').find('.edit-switch-text-area .edit-switch-editor button.save').click();

      cy.wait('@updateComment').its('response.statusCode').should('eq', 200);
      cy.get(toastSuccess).should('be.visible').and('contain.text', 'Data stored successfully');
      cy.get('@transfer').find('.edit-switch-text-area .edit-switch-label span').should('contain.text', comment);
    });

    it('should delete only the first data transfer and leave the second intact', () => {
      cy.intercept('POST', '/dataTransfer/deleteDataTransfer*').as('deleteTransfer');

      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();

      // Save the document-container ID of the second (last) transfer before any deletion
      cy.get('@dtaCard').find('.transfer-listing > li').last()
        .find('[id^="transferDocuments-"]')
        .invoke('attr', 'id')
        .as('remainingTransferDocId');

      // Delete the first transfer
      cy.on('window:confirm', () => true);
      cy.get('@dtaCard').find('.transfer-listing > li').first()
        .find('.delete-transfer-wrapper button[type=submit]').click();

      cy.wait('@deleteTransfer').its('response.statusCode').should('eq', 302);

      // Verify exactly one transfer remains and it is the correct one
      cy.get('@remainingTransferDocId').then((remainingId) => {
        cy.contains('#dtaAccordion .card', peerInstitution)
          .find('.delete-transfer-wrapper button[type=submit]').should('have.length', 1);
        cy.contains('#dtaAccordion .card', peerInstitution)
          .find(`#${remainingId}`).should('exist');
      });
    });

    it('should delete the data transfer agreement', () => {
      cy.intercept('POST', '/dataTransfer/deleteDataTransferAgreement*').as('deleteDta');

      cy.contains('#dtaAccordion .card', peerInstitution).as('dtaCard');
      cy.get('@dtaCard').find('.btn-link').click();

      cy.on('window:confirm', () => true);
      cy.get('@dtaCard').find('form[action*="deleteDataTransferAgreement"] button[type=submit]').click();

      cy.wait('@deleteDta').its('response.statusCode').should('eq', 302);
      cy.get('#dtaAccordion').should('not.contain.text', peerInstitution);
    });
  });

  context('when user is a normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/dataTransfer/index');
    });
  });
});
