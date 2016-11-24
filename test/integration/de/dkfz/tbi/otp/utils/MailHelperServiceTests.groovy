package de.dkfz.tbi.otp.utils

import grails.plugin.mail.MailService
import de.dkfz.tbi.TestCase
import org.junit.After
import org.junit.Test

class MailHelperServiceTests {

    static final String SUBJECT = 'Test Subject'
    static final String BODY = 'Test Body'
    static final String RECIPIENT = 'a.b@c.d'

    MailHelperService mailHelperService

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

    @Test
    void testSendNotificationEmail() {
        String oldRecipient = mailHelperService.grailsApplication.config.otp.mail.notification.to
        try {
            mailHelperService.grailsApplication.config.otp.mail.notification.to = RECIPIENT

            mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
                assert SUBJECT == subject
                assert BODY == content
                assert RECIPIENT == recipient
            }

            mailHelperService.sendNotificationEmail(SUBJECT, BODY)
        } finally {
            mailHelperService.grailsApplication.config.otp.mail.notification.to = oldRecipient
        }
    }

}
