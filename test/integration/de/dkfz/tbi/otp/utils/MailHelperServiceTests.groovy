package de.dkfz.tbi.otp.utils

import grails.plugin.mail.MailService
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

class MailHelperServiceTests {

    static final String SUBJECT = 'Test Subject'
    static final String BODY = 'Test Body'
    static final String RECIPIENT = 'a.b@c.d'

    MailHelperService mailHelperService

    ProcessingOptionService processingOptionService

    @After
    void tearDown() {
        TestCase.removeMetaClass(MailHelperService, mailHelperService)
        TestCase.removeMetaClass(MailService, mailHelperService.mailService)
    }

    @Test
    void testSendEmail() {
        mailHelperService.sendEmail(
                SUBJECT,
                BODY,
                RECIPIENT,
        )
    }

    @Test
    void testSendEmailWithMultipleRecipients() {
        mailHelperService.sendEmail(
                SUBJECT,
                BODY,
                [RECIPIENT, RECIPIENT],
        )
    }

    @Test
    void testSendEmail_subjectIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            mailHelperService.sendEmail(
                    null,
                    BODY,
                    RECIPIENT,
            )
        }.contains('assert emailSubject')
    }

    @Test
    void testSendEmail_bodyIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            mailHelperService.sendEmail(
                    SUBJECT,
                    null,
                    RECIPIENT,
            )
        }.contains('assert content')
    }

    @Test
    void testSendEmail_receiverIsNull_shouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            mailHelperService.sendEmail(
                    SUBJECT,
                    BODY,
                    null,
            )
        }.contains('assert recipient')
    }
}
